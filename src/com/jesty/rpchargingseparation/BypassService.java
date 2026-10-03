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
import android.os.PowerManager;
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

    static final String PREF_MODE = "separation_mode";
    static final String MODE_IMMEDIATE = "immediate";
    static final String MODE_AUTO_LIMIT = "auto_limit";
    static final String PREF_LIMIT_PERCENT = "limit_percent";
    static final String PREF_RESUME_PERCENT = "resume_percent";
    static final String PREF_LAST_SAFETY_STOP = "last_safety_stop";
    // Pre-release builds stored a margin instead of an absolute resume level.
    private static final String LEGACY_PREF_RESUME_MARGIN = "resume_margin";
    static final int DEFAULT_LIMIT_PERCENT = 80;
    static final int DEFAULT_RESUME_PERCENT = 70;
    static final int MIN_LIMIT_PERCENT = 30;
    static final int MAX_LIMIT_PERCENT = 100;
    static final int MIN_RESUME_PERCENT = ChargeLimitPolicy.MIN_RESUME_PERCENT;
    static final int MIN_RESUME_GAP = ChargeLimitPolicy.MIN_RESUME_GAP;

    enum State { OFF, ARMED, ENABLING, ACTIVE, CHARGING_TO_LIMIT, FAILED }

    private static volatile State state = State.OFF;
    private static volatile String detail = "Bypass charging is off";

    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private ScheduledExecutorService worker;
    private SharedPreferences prefs;
    private int unsafeSamples;
    private boolean monitorScheduled;
    // Auto-limit hysteresis: true once the limit was reached, until the resume threshold.
    private boolean limitReached;
    private PowerManager.WakeLock chargeToLimitWakeLock;

    static State state() { return state; }
    static String detail() { return detail; }

    static boolean autoLimitMode(SharedPreferences prefs) {
        return MODE_AUTO_LIMIT.equals(prefs.getString(PREF_MODE, MODE_IMMEDIATE));
    }

    static int limitPercent(SharedPreferences prefs) {
        return clamp(prefs.getInt(PREF_LIMIT_PERCENT, DEFAULT_LIMIT_PERCENT),
                MIN_LIMIT_PERCENT, MAX_LIMIT_PERCENT);
    }

    static int resumePercent(SharedPreferences prefs) {
        int limit = limitPercent(prefs);
        int stored;
        if (prefs.contains(PREF_RESUME_PERCENT)) {
            stored = prefs.getInt(PREF_RESUME_PERCENT, DEFAULT_RESUME_PERCENT);
        } else if (prefs.contains(LEGACY_PREF_RESUME_MARGIN)) {
            stored = ChargeLimitPolicy.migrateResumeMargin(
                    limit, prefs.getInt(LEGACY_PREF_RESUME_MARGIN, 10));
            prefs.edit().putInt(PREF_RESUME_PERCENT, stored)
                    .remove(LEGACY_PREF_RESUME_MARGIN).apply();
        } else {
            stored = DEFAULT_RESUME_PERCENT;
        }
        return clampResumePercent(stored, limit);
    }

    static int clampResumePercent(int resume, int limit) {
        return ChargeLimitPolicy.clampResumePercent(resume, limit);
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("state", MODE_PRIVATE);
        worker = Executors.newSingleThreadScheduledExecutor();
        PowerManager power = getSystemService(PowerManager.class);
        chargeToLimitWakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                TAG + ":charge-to-limit");
        chargeToLimitWakeLock.setReferenceCounted(false);
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean desired = prefs.getBoolean("desired_enabled", false);
        String action = intent == null ? "sticky restart" : intent.getAction();
        Log.i(TAG, "Service start: " + action + ", desired=" + desired
                + ", flags=" + flags);
        // The saved user choice is authoritative, including for a sticky restart
        // with a null intent or an old ENABLE intent delivered after an OFF.
        if (desired) {
            state = State.ENABLING;
            detail = "Checking hardware...";
            startForeground(NOTIFICATION_ID, notification(detail));
            worker.execute(this::enableSafely);
        } else {
            startForeground(NOTIFICATION_ID, notification("Restoring normal charging..."));
            worker.execute(() -> disableSafely("Bypass charging is off", true));
        }
        return START_STICKY;
    }

    private void enableSafely() {
        try {
            stopping.set(false);
            if (!prefs.getBoolean("desired_enabled", false)) {
                disableSafely("Bypass charging is off", true);
                return;
            }
            requireSupportedDevice();
            PowerTelemetry before = PowerTelemetry.read();
            if (!before.usbPresent) {
                if (before.limit != 0) restoreAndConfirm();
                releaseChargeToLimitWakeLock();
                state = State.ARMED;
                detail = "Plug in USB to start";
                updateNotification(detail);
                ensureMonitor();
                return;
            }
            // USB can appear before a dock finishes negotiating power. Do not
            // treat its initial Discharging/Unknown status as a hardware failure.
            for (int attempt = 0; attempt < 10 && before.usbPresent
                    && !powerReady(before); attempt++) {
                detail = "Waiting for charger...";
                updateNotification(detail);
                Thread.sleep(1000L);
                before = PowerTelemetry.read();
            }
            if (!prefs.getBoolean("desired_enabled", false)) {
                disableSafely("Bypass charging is off", true);
                return;
            }
            if (!before.usbPresent) {
                if (before.limit != 0) restoreAndConfirm();
                releaseChargeToLimitWakeLock();
                state = State.ARMED;
                detail = "Plug in USB to start";
                updateNotification(detail);
                ensureMonitor();
                return;
            }
            if (!powerReady(before)) {
                throw new IllegalStateException("Charger did not settle: "
                        + before.batteryStatus);
            }
            if (before.limitMax <= 0) throw new IllegalStateException("Invalid native limit");
            if (before.limit != 0 && before.limit != before.limitMax) {
                throw new IllegalStateException("Unexpected native limit: " + before.limit);
            }

            // A process restart loses the in-memory hysteresis flag. The native
            // Retroid setting tells us whether the stop level was already reached.
            if (autoLimitMode(prefs) && before.nativeIdleMode()) limitReached = true;

            if (shouldChargeToLimit(before)) {
                enterLimitCharging(before);
                return;
            }

            prefs.edit().putBoolean("requested", true).apply();
            PowerTelemetry after = before;
            if (!before.nativeIdleMode()) {
                RootBridge.exec("chmod 644 " + PowerTelemetry.LIMIT);
                RootBridge.exec("echo " + before.limitMax + " > " + PowerTelemetry.LIMIT);
                RootBridge.exec("chmod 444 " + PowerTelemetry.LIMIT);
                Thread.sleep(1500L);
                after = PowerTelemetry.read();
            }
            for (int attempt = 0; attempt < 8 && after.usbPresent
                    && after.limit == after.limitMax && !after.nativeIdleMode(); attempt++) {
                detail = "Confirming bypass...";
                updateNotification(detail);
                Thread.sleep(1000L);
                after = PowerTelemetry.read();
            }
            if (!prefs.getBoolean("desired_enabled", false)) {
                disableSafely("Bypass charging is off", true);
                return;
            }
            if (!after.usbPresent || after.limit != after.limitMax
                    || !"Not charging".equalsIgnoreCase(after.batteryStatus)) {
                throw new IllegalStateException("Validation failed: limit " + after.limit + "/"
                        + after.limitMax + ", status " + after.batteryStatus);
            }

            state = State.ACTIVE;
            limitReached = autoLimitMode(prefs);
            releaseChargeToLimitWakeLock();
            Log.i(TAG, before.nativeIdleMode()
                    ? "Reattached to existing native separation"
                    : "Enabled and verified native separation");
            detail = autoLimitMode(prefs)
                    ? String.format(Locale.US,
                            "Stopped at %d%% - charges again at %d%%",
                            limitPercent(prefs), resumePercent(prefs))
                    : "Battery is not charging";
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

    private boolean shouldChargeToLimit(PowerTelemetry telemetry) {
        return ChargeLimitPolicy.shouldChargeToLimit(autoLimitMode(prefs),
                telemetry.batteryPercent, limitPercent(prefs), resumePercent(prefs),
                limitReached);
    }

    private static boolean powerReady(PowerTelemetry telemetry) {
        return "Charging".equalsIgnoreCase(telemetry.batteryStatus)
                || "Full".equalsIgnoreCase(telemetry.batteryStatus)
                || "Not charging".equalsIgnoreCase(telemetry.batteryStatus);
    }

    private void enterLimitCharging(PowerTelemetry telemetry) throws Exception {
        if (telemetry.limit != 0) restoreAndConfirm();
        holdChargeToLimitWakeLock();
        limitReached = false;
        unsafeSamples = 0;
        state = State.CHARGING_TO_LIMIT;
        updateLimitChargingDetail(telemetry);
        ensureMonitor();
    }

    private void updateLimitChargingDetail(PowerTelemetry telemetry) {
        detail = String.format(Locale.US,
                "Will stop at %d%% (now %d%%)",
                limitPercent(prefs), telemetry.batteryPercent);
        updateNotification(detail);
    }

    private void monitor() {
        if (stopping.get()) return;
        try {
            PowerTelemetry telemetry = PowerTelemetry.read();
            boolean desired = prefs.getBoolean("desired_enabled", false);
            if (!desired) {
                disableSafely("Bypass charging is off", true);
                return;
            }
            if (state == State.ARMED) {
                if (telemetry.usbPresent) enableSafely();
                return;
            }
            if (state == State.CHARGING_TO_LIMIT) {
                if (!telemetry.usbPresent) {
                    releaseChargeToLimitWakeLock();
                    state = State.ARMED;
                    detail = "Plug in USB to start";
                    updateNotification(detail);
                    return;
                }
                if (telemetry.limit != 0) {
                    disableSafely("Control changed externally", true);
                    return;
                }
                if (shouldChargeToLimit(telemetry)) updateLimitChargingDetail(telemetry);
                else enableSafely();
                return;
            }
            if (state != State.ACTIVE) return;
            if (!telemetry.usbPresent) {
                releaseChargeToLimitWakeLock();
                state = State.ARMED;
                detail = "Plug in USB to start";
                updateNotification(detail);
                return;
            }
            if (telemetry.limit != telemetry.limitMax) {
                disableSafely("Control changed externally", true);
                return;
            }
            if (autoLimitMode(prefs)) {
                if (telemetry.batteryPercent <= resumePercent(prefs)) limitReached = false;
                if (shouldChargeToLimit(telemetry)) {
                    enterLimitCharging(telemetry);
                    return;
                }
            }

            if (telemetry.batteryCurrentUa < -300_000L) unsafeSamples++;
            else unsafeSamples = 0;
            if (unsafeSamples >= 3) {
                Log.w(TAG, "Safety stop: battery powering device at "
                        + telemetry.batteryCurrentUa + " uA while USB is present");
                prefs.edit().putString(PREF_LAST_SAFETY_STOP,
                        "Battery was powering the device")
                        .putBoolean("desired_enabled", false).commit();
                disableSafely("Battery was powering the device", true);
                return;
            }

            detail = autoLimitMode(prefs)
                    ? String.format(Locale.US,
                            "Stopped at %d%% - charges again at %d%% - USB %.1f W",
                            limitPercent(prefs), resumePercent(prefs), telemetry.usbWatts())
                    : String.format(Locale.US,
                            "Battery is not charging - USB %.1f W",
                            telemetry.usbWatts());
            updateNotification(detail);
        } catch (Throwable error) {
            Log.e(TAG, "Monitoring failed", error);
            disableSafely("Telemetry error", true);
            if (state == State.OFF) {
                state = State.FAILED;
                detail = "Telemetry error";
            }
        }
    }

    private void ensureMonitor() {
        if (monitorScheduled) return;
        monitorScheduled = true;
        worker.scheduleAtFixedRate(this::monitor, 0L, 2L, TimeUnit.SECONDS);
    }

    private void holdChargeToLimitWakeLock() {
        if (!chargeToLimitWakeLock.isHeld()) {
            chargeToLimitWakeLock.acquire();
            Log.i(TAG, "Holding CPU awake until the charge limit is reached");
        }
    }

    private void releaseChargeToLimitWakeLock() {
        if (chargeToLimitWakeLock != null && chargeToLimitWakeLock.isHeld()) {
            chargeToLimitWakeLock.release();
            Log.i(TAG, "Released charge-limit wake lock");
        }
    }

    private void restoreHardware() throws Exception {
        RootBridge.exec("chmod 644 " + PowerTelemetry.LIMIT);
        RootBridge.exec("echo 0 > " + PowerTelemetry.LIMIT);
        RootBridge.exec("chmod 444 " + PowerTelemetry.LIMIT);
    }

    private void restoreAndConfirm() throws Exception {
        restoreHardware();
        String readback = RootBridge.exec("cat " + PowerTelemetry.LIMIT).trim();
        if (!"0".equals(readback)) {
            throw new IllegalStateException("Restore not confirmed: " + readback);
        }
        prefs.edit().putBoolean("requested", false).apply();
    }

    private void disableSafely(String reason, boolean stop) {
        if (!stopping.compareAndSet(false, true) && stop) return;
        releaseChargeToLimitWakeLock();
        try {
            restoreAndConfirm();
            limitReached = false;
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
                        || state == State.ARMED
                        || state == State.CHARGING_TO_LIMIT)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                "Bypass charging", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Bypass charging state");
        channel.enableLights(false);
        channel.setLightColor(Color.YELLOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private void updateNotification(String text) {
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification(text));
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "Task removed; no change to bypass requested");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed: state=" + state + ", desired="
                + prefs.getBoolean("desired_enabled", false));
        releaseChargeToLimitWakeLock();
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
