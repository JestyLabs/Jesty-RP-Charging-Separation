package com.jesty.rpchargingseparation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.jesty.rpchargingseparation.BypassController.State;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BypassService extends Service {
    static final String ACTION_ENABLE = "com.jesty.rpchargingseparation.ENABLE";
    static final String ACTION_DISABLE = "com.jesty.rpchargingseparation.DISABLE";
    static final String CHANNEL = "charging_separation";
    static final int NOTIFICATION_ID = 4102;
    static final String TAG = "JestyRPCharging";

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

    static final String PREF_DESIRED = "desired_enabled";
    static final String PREF_REQUESTED = "requested";
    // Upper bound for one event-triggered evaluation, including charger settling.
    private static final long EVENT_WAKE_MS = 60_000L;
    private static final long POLL_SECONDS = 2L;

    private static volatile State state = State.OFF;
    private static volatile String detail = BypassController.OFF_REASON;

    private final AtomicBoolean tickQueued = new AtomicBoolean(false);
    private ScheduledExecutorService worker;
    private SharedPreferences prefs;
    private BypassController controller;
    private EventLog events;
    private PowerManager.WakeLock chargeToLimitWakeLock;
    private PowerManager.WakeLock eventWakeLock;
    private boolean monitorScheduled;
    private boolean receiverRegistered;
    private int lastEventKey = Integer.MIN_VALUE;

    static State state() { return state; }
    static String detail() { return detail; }

    private final BroadcastReceiver powerEvents = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
                int percent = level < 0 || scale <= 0 ? -1 : level * 100 / scale;
                int plugged = intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0);
                // BATTERY_CHANGED also fires for voltage/temperature; react to level or plug.
                int key = percent * 16 + plugged;
                if (key == lastEventKey) return;
                lastEventKey = key;
            }
            scheduleTick(action == null ? "event" : shortAction(action), true);
        }
    };

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

    static EventLog events(Context context) {
        return new EventLog(context.getFilesDir());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("state", MODE_PRIVATE);
        events = events(this);
        worker = Executors.newSingleThreadScheduledExecutor();
        PowerManager power = getSystemService(PowerManager.class);
        chargeToLimitWakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                TAG + ":charge-to-limit");
        chargeToLimitWakeLock.setReferenceCounted(false);
        eventWakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":power-event");
        eventWakeLock.setReferenceCounted(false);
        controller = new BypassController(new SysfsHardware(), new PrefsSettings(),
                new AndroidPlatform());
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean desired = prefs.getBoolean(PREF_DESIRED, false);
        String action = intent == null ? "sticky restart" : intent.getAction();
        Log.i(TAG, "Service start: " + action + ", desired=" + desired
                + ", flags=" + flags);
        events.add(System.currentTimeMillis(), "Service start: " + action
                + ", desired=" + desired + ", flags=" + flags);
        // The saved user choice is authoritative, including for a sticky restart
        // with a null intent or an old ENABLE intent delivered after an OFF.
        if (desired) {
            state = State.ENABLING;
            detail = "Checking hardware...";
            startForeground(NOTIFICATION_ID, notification(detail));
            registerPowerEvents();
            ensureMonitor();
        } else {
            startForeground(NOTIFICATION_ID, notification("Restoring normal charging..."));
        }
        String source = action == null ? "start" : shortAction(action);
        worker.execute(() -> controller.start(source));
        return START_STICKY;
    }

    /**
     * Event-driven path for issue #2: the 2 s timer uses a clock that stops in deep
     * sleep, so a plug-in or a battery level step while asleep could go unnoticed until
     * the app was reopened. Each event briefly holds the CPU and re-evaluates the state.
     */
    private void registerPowerEvents() {
        if (receiverRegistered) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(powerEvents, filter);
        receiverRegistered = true;
    }

    private void scheduleTick(String source, boolean fromEvent) {
        if (worker == null || worker.isShutdown()) return;
        if (fromEvent) eventWakeLock.acquire(EVENT_WAKE_MS);
        if (!tickQueued.compareAndSet(false, true)) return;
        try {
            worker.execute(() -> {
                tickQueued.set(false);
                try {
                    controller.tick(source);
                } finally {
                    if (!tickQueued.get() && eventWakeLock.isHeld()) eventWakeLock.release();
                }
            });
        } catch (RuntimeException rejected) {
            tickQueued.set(false);
        }
    }

    private void ensureMonitor() {
        if (monitorScheduled) return;
        monitorScheduled = true;
        worker.scheduleAtFixedRate(() -> scheduleTick("poll", false),
                POLL_SECONDS, POLL_SECONDS, TimeUnit.SECONDS);
    }

    private static String shortAction(String action) {
        int dot = action.lastIndexOf('.');
        return dot < 0 ? action : action.substring(dot + 1);
    }

    private void endService(boolean keepNotice) {
        if (keepNotice) {
            // Keep the failure reason visible after the service ends.
            updateNotification(detail);
            stopForeground(STOP_FOREGROUND_DETACH);
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE);
        }
        stopSelf();
    }

    private final class SysfsHardware implements BypassController.Hardware {
        @Override
        public String missingControls() {
            File limit = new File(PowerTelemetry.LIMIT);
            File maximum = new File(PowerTelemetry.LIMIT_MAX);
            StringBuilder missing = new StringBuilder();
            if (!limit.isFile() || !limit.canRead()) missing.append("charge_control_limit");
            if (!maximum.isFile() || !maximum.canRead()) {
                if (missing.length() > 0) missing.append(", ");
                missing.append("charge_control_limit_max");
            }
            return missing.length() == 0 ? null
                    : "Unsupported on " + Build.MODEL + ": missing " + missing;
        }

        @Override
        public PowerTelemetry read() throws Exception {
            return PowerTelemetry.read();
        }

        @Override
        public void writeLimit(int value) throws Exception {
            RootBridge.exec("chmod 644 " + PowerTelemetry.LIMIT);
            RootBridge.exec("echo " + value + " > " + PowerTelemetry.LIMIT);
            RootBridge.exec("chmod 444 " + PowerTelemetry.LIMIT);
        }

        @Override
        public String readLimitRaw() throws Exception {
            return RootBridge.exec("cat " + PowerTelemetry.LIMIT);
        }
    }

    private final class PrefsSettings implements BypassController.Settings {
        @Override public boolean desired() { return prefs.getBoolean(PREF_DESIRED, false); }
        @Override public boolean autoLimit() { return autoLimitMode(prefs); }
        @Override public int limitPercent() { return BypassService.limitPercent(prefs); }
        @Override public int resumePercent() { return BypassService.resumePercent(prefs); }

        @Override
        public void setRequested(boolean requested) {
            prefs.edit().putBoolean(PREF_REQUESTED, requested).apply();
        }

        @Override
        public void recordSafetyStop(String reason) {
            prefs.edit().putString(PREF_LAST_SAFETY_STOP, reason)
                    .putBoolean(PREF_DESIRED, false).commit();
        }
    }

    private final class AndroidPlatform implements BypassController.Platform {
        @Override public long nowMillis() { return SystemClock.elapsedRealtime(); }

        @Override
        public void sleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }

        @Override
        public void setChargeWakeLock(boolean held) {
            if (held && !chargeToLimitWakeLock.isHeld()) chargeToLimitWakeLock.acquire();
            if (!held && chargeToLimitWakeLock.isHeld()) chargeToLimitWakeLock.release();
        }

        @Override
        public void publish(State next, String nextDetail) {
            state = next;
            detail = nextDetail;
            updateNotification(nextDetail);
        }

        @Override
        public void stopService(boolean keepNotice) {
            endService(keepNotice);
        }

        @Override
        public void log(String message) {
            Log.i(TAG, message);
            events.add(System.currentTimeMillis(), message);
        }
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
                        || state == State.CHARGING_TO_LIMIT
                        || state == State.RETRYING)
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
        if (events != null) events.add(System.currentTimeMillis(),
                "Task removed; no change to bypass requested");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(powerEvents);
            receiverRegistered = false;
        }
        if (worker != null) {
            // Interrupt any enable in progress, then restore on this thread.
            worker.shutdownNow();
            try {
                worker.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        if (controller != null) controller.destroy();
        if (chargeToLimitWakeLock.isHeld()) chargeToLimitWakeLock.release();
        if (eventWakeLock.isHeld()) eventWakeLock.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
