package com.jesty.rpchargingseparation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.os.Build;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BypassService extends Service {
    static final String ACTION_ENABLE = "com.jesty.rpchargingseparation.ENABLE";
    static final String ACTION_DISABLE = "com.jesty.rpchargingseparation.DISABLE";
    static final String CHANNEL = "charging_separation";
    static final int NOTIFICATION_ID = 4102;
    private static final String TAG = "JestyRPCharging";

    enum State { OFF, ARMED, ENABLING, ACTIVE, FAILED }

    private static volatile State state = State.OFF;
    private static volatile String detail = "Normal charging";

    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private ScheduledExecutorService worker;
    private SharedPreferences prefs;
    private int unsafeSamples;
    private boolean monitorScheduled;

    static State state() { return state; }
    static String detail() { return detail; }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("state", MODE_PRIVATE);
        worker = Executors.newSingleThreadScheduledExecutor();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null
                ? (prefs.getBoolean("desired_enabled", false) ? ACTION_ENABLE : ACTION_DISABLE)
                : intent.getAction();
        if (ACTION_ENABLE.equals(action)) {
            state = State.ENABLING;
            detail = "Validating native charging separation...";
            startForeground(NOTIFICATION_ID, notification(detail));
            worker.execute(this::enableSafely);
        } else {
            startForeground(NOTIFICATION_ID, notification("Restoring normal charging..."));
            worker.execute(() -> disableSafely("Disabled by user", true));
        }
        return START_STICKY;
    }

    private void enableSafely() {
        try {
            stopping.set(false);
            requireSupportedDevice();
            PowerTelemetry before = PowerTelemetry.read();
            if (!before.usbPresent) {
                state = State.ARMED;
                detail = "Armed - waiting for USB/PD";
                updateNotification(detail);
                ensureMonitor();
                return;
            }
            if (before.limitMax <= 0) throw new IllegalStateException("Invalid native limit");

            prefs.edit().putBoolean("requested", true).apply();
            RootBridge.exec("chmod 644 " + PowerTelemetry.LIMIT);
            RootBridge.exec("echo " + before.limitMax + " > " + PowerTelemetry.LIMIT);
            RootBridge.exec("chmod 444 " + PowerTelemetry.LIMIT);

            Thread.sleep(1500L);
            PowerTelemetry after = PowerTelemetry.read();
            if (after.limit != after.limitMax
                    || !"Not charging".equalsIgnoreCase(after.batteryStatus)) {
                throw new IllegalStateException("Validation failed: limit " + after.limit + "/"
                        + after.limitMax + ", status " + after.batteryStatus);
            }

            state = State.ACTIVE;
            detail = "Charging separation active and validated";
            updateNotification(detail);
            ensureMonitor();
        } catch (Throwable error) {
            Log.e(TAG, "Enable failed", error);
            detail = error.getMessage() == null ? error.toString() : error.getMessage();
            String failure = detail;
            disableSafely(failure, true);
            state = State.FAILED;
            detail = failure;
        }
    }

    private void monitor() {
        if (stopping.get()) return;
        try {
            PowerTelemetry telemetry = PowerTelemetry.read();
            boolean desired = prefs.getBoolean("desired_enabled", false);
            if (!desired) {
                disableSafely("Disabled by user", true);
                return;
            }
            if (state == State.ARMED) {
                if (telemetry.usbPresent) enableSafely();
                return;
            }
            if (state != State.ACTIVE) return;
            if (!telemetry.usbPresent) {
                state = State.ARMED;
                detail = "Armed - waiting for USB/PD";
                updateNotification(detail);
                return;
            }
            if (telemetry.limit != telemetry.limitMax) {
                disableSafely("Control changed externally", true);
                return;
            }

            if (telemetry.batteryCurrentUa < -300_000L) unsafeSamples++;
            else unsafeSamples = 0;
            if (unsafeSamples >= 3) {
                prefs.edit().putBoolean("desired_enabled", false).apply();
                disableSafely("Battery was powering the device", true);
                return;
            }

            detail = String.format(Locale.US,
                    "Separated - USB %.1f W - Battery %+.0f mA",
                    telemetry.usbWatts(), telemetry.displayedBatteryCurrentUa() / 1000d);
            updateNotification(detail);
        } catch (Throwable error) {
            disableSafely("Telemetry error", true);
        }
    }

    private void ensureMonitor() {
        if (monitorScheduled) return;
        monitorScheduled = true;
        worker.scheduleAtFixedRate(this::monitor, 0L, 2L, TimeUnit.SECONDS);
    }

    private void restoreHardware() throws Exception {
        RootBridge.exec("chmod 644 " + PowerTelemetry.LIMIT);
        RootBridge.exec("echo 0 > " + PowerTelemetry.LIMIT);
        RootBridge.exec("chmod 444 " + PowerTelemetry.LIMIT);
    }

    private void disableSafely(String reason, boolean stop) {
        if (!stopping.compareAndSet(false, true) && stop) return;
        try {
            restoreHardware();
            String readback = RootBridge.exec("cat " + PowerTelemetry.LIMIT).trim();
            if (!"0".equals(readback)) {
                throw new IllegalStateException("Restore not confirmed: " + readback);
            }
            prefs.edit().putBoolean("requested", false).apply();
            state = State.OFF;
            detail = reason;
        } catch (Throwable error) {
            state = State.FAILED;
            detail = "Critical restore failure: " + error.getMessage();
            Log.e(TAG, detail, error);
        } finally {
            if (stop) {
                updateNotification(detail);
                stopForeground(true);
                stopSelf();
            }
        }
    }

    private void requireSupportedDevice() {
        java.io.File limit = new java.io.File(PowerTelemetry.LIMIT);
        java.io.File maximum = new java.io.File(PowerTelemetry.LIMIT_MAX);
        StringBuilder missing = new StringBuilder();
        if (!limit.isFile() || !limit.canRead()) missing.append("charge_control_limit");
        if (!maximum.isFile() || !maximum.canRead()) {
            if (missing.length() > 0) missing.append(", ");
            missing.append("charge_control_limit_max");
        }
        if (missing.length() > 0) throw new IllegalStateException(
                "Unsupported on " + Build.MODEL + ": missing " + missing);
    }

    private Notification notification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_charging_separation)
                .setContentTitle("Jesty RP Charging Separation")
                .setContentText(text)
                .setContentIntent(pending)
                .setOngoing(state == State.ACTIVE
                        || state == State.ENABLING
                        || state == State.ARMED)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                "Charging separation", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Retroid native charging-separation state");
        channel.enableLights(false);
        channel.setLightColor(Color.YELLOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private void updateNotification(String text) {
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification(text));
    }

    @Override
    public void onDestroy() {
        if (state == State.ACTIVE || state == State.ENABLING) {
            disableSafely("Service stopped", false);
        }
        if (worker != null) worker.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
