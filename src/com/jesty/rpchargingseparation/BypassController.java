package com.jesty.rpchargingseparation;

import java.util.Locale;

/**
 * Bypass state machine without Android dependencies so it can be tested on a plain JDK.
 * All methods must be called from one worker thread; {@link BypassService} provides the
 * Android adapters, the polling timer, and the battery/power broadcast events.
 */
final class BypassController {
    enum State { OFF, ARMED, ENABLING, ACTIVE, CHARGING_TO_LIMIT, RETRYING, FAILED }

    interface Hardware {
        /** Returns null when the native controls exist, otherwise why the device is unsupported. */
        String missingControls();
        PowerTelemetry read() throws Exception;
        void writeLimit(int value) throws Exception;
        String readLimitRaw() throws Exception;
    }

    interface Settings {
        boolean desired();
        boolean autoLimit();
        int limitPercent();
        int resumePercent();
        void setRequested(boolean requested);
        /** Persists desired=false together with the reason shown after a safety stop. */
        void recordSafetyStop(String reason);
    }

    interface Platform {
        /** Monotonic time that keeps counting while the device sleeps. */
        long nowMillis();
        void sleep(long millis) throws InterruptedException;
        void setChargeWakeLock(boolean held);
        void publish(State state, String detail);
        /** Ends the service; keepNotice leaves the last notification visible. */
        void stopService(boolean keepNotice);
        void log(String message);
    }

    static final String OFF_REASON = "Bypass charging is off";
    static final String NO_USB_DETAIL = "Plug in USB to start";
    static final String SAFETY_STOP_REASON = "Battery was powering the device";
    static final long FIRST_RETRY_DELAY_MS = 15_000L;
    static final long MAX_RETRY_DELAY_MS = 5L * 60L * 1000L;
    static final long UNSAFE_BATTERY_CURRENT_UA = -300_000L;
    static final int UNSAFE_SAMPLES_BEFORE_STOP = 3;

    /** Failures that retrying cannot fix, such as missing native controls. */
    static final class PermanentFailure extends Exception {
        PermanentFailure(String message) { super(message); }
    }

    private final Hardware hardware;
    private final Settings settings;
    private final Platform platform;

    private volatile State state = State.OFF;
    private volatile String detail = OFF_REASON;
    private boolean stopping;
    private boolean limitReached;
    private boolean wakeLockHeld;
    private int unsafeSamples;
    private int retryAttempts;
    private long nextRetryAt;

    BypassController(Hardware hardware, Settings settings, Platform platform) {
        this.hardware = hardware;
        this.settings = settings;
        this.platform = platform;
    }

    State state() { return state; }
    String detail() { return detail; }
    boolean wakeLockHeld() { return wakeLockHeld; }
    int retryAttempts() { return retryAttempts; }

    static long retryDelayMs(int attempt) {
        long delay = FIRST_RETRY_DELAY_MS;
        for (int i = 1; i < attempt && delay < MAX_RETRY_DELAY_MS; i++) delay *= 2;
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }

    /** Applies the saved user choice; used for explicit starts and sticky restarts. */
    void start(String source) {
        platform.log("Start (" + source + "), desired=" + settings.desired());
        if (settings.desired()) {
            enable();
        } else {
            disable(OFF_REASON, true);
        }
    }

    void enable() {
        try {
            stopping = false;
            if (!settings.desired()) {
                disable(OFF_REASON, true);
                return;
            }
            transition(State.ENABLING, "Checking hardware...");
            String missing = hardware.missingControls();
            if (missing != null) throw new PermanentFailure(missing);

            PowerTelemetry before = hardware.read();
            if (!before.usbPresent) {
                arm(before);
                return;
            }
            // USB can appear before a dock finishes negotiating power. Do not
            // treat its initial Discharging/Unknown status as a hardware failure.
            for (int attempt = 0; attempt < 10 && before.usbPresent
                    && !powerReady(before); attempt++) {
                transition(State.ENABLING, "Waiting for charger...");
                platform.sleep(1000L);
                before = hardware.read();
            }
            if (!settings.desired()) {
                disable(OFF_REASON, true);
                return;
            }
            if (!before.usbPresent) {
                arm(before);
                return;
            }
            if (!powerReady(before)) {
                throw new IllegalStateException("Charger did not settle: " + before.batteryStatus);
            }
            if (before.limitMax <= 0) throw new IllegalStateException("Invalid native limit");
            if (before.limit != 0 && before.limit != before.limitMax) {
                throw new IllegalStateException("Unexpected native limit: " + before.limit);
            }

            // A process restart loses the in-memory hysteresis flag. The native
            // Retroid setting tells us whether the stop level was already reached.
            if (settings.autoLimit() && before.nativeIdleMode()) limitReached = true;

            if (shouldChargeToLimit(before)) {
                enterLimitCharging(before);
                return;
            }

            settings.setRequested(true);
            PowerTelemetry after = before;
            if (!before.nativeIdleMode()) {
                hardware.writeLimit(before.limitMax);
                platform.sleep(1500L);
                after = hardware.read();
            }
            for (int attempt = 0; attempt < 8 && after.usbPresent
                    && after.limit == after.limitMax && !after.nativeIdleMode(); attempt++) {
                transition(State.ENABLING, "Confirming bypass...");
                platform.sleep(1000L);
                after = hardware.read();
            }
            if (!settings.desired()) {
                disable(OFF_REASON, true);
                return;
            }
            if (!after.usbPresent || after.limit != after.limitMax
                    || !"Not charging".equalsIgnoreCase(after.batteryStatus)) {
                throw new IllegalStateException("Validation failed: limit " + after.limit + "/"
                        + after.limitMax + ", status " + after.batteryStatus);
            }

            limitReached = settings.autoLimit();
            retryAttempts = 0;
            unsafeSamples = 0;
            setWakeLock(false);
            platform.log((before.nativeIdleMode()
                    ? "Reattached to existing native separation"
                    : "Enabled and verified native separation")
                    + " at " + after.batteryPercent + "%");
            transition(State.ACTIVE, activeDetail(after, false));
        } catch (PermanentFailure error) {
            failPermanently(error.getMessage());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (Throwable error) {
            retryAfterFailure(message(error));
        }
    }

    /** Re-evaluates the state; called by the poll timer and by battery/power events. */
    void tick(String source) {
        if (stopping) return;
        try {
            PowerTelemetry telemetry = hardware.read();
            if (!settings.desired()) {
                disable(OFF_REASON, true);
                return;
            }
            switch (state) {
                case RETRYING:
                    if (!telemetry.usbPresent) {
                        arm(telemetry);
                    } else if (platform.nowMillis() >= nextRetryAt) {
                        platform.log("Retry " + retryAttempts + " (" + source + ")");
                        enable();
                    }
                    return;
                case ARMED:
                    if (telemetry.usbPresent) {
                        platform.log("USB detected (" + source + ") at "
                                + telemetry.batteryPercent + "%");
                        enable();
                    }
                    return;
                case CHARGING_TO_LIMIT:
                    if (!telemetry.usbPresent) {
                        arm(telemetry);
                        return;
                    }
                    if (telemetry.limit != 0) {
                        disable("Control changed externally", true);
                        return;
                    }
                    if (shouldChargeToLimit(telemetry)) {
                        updateLimitChargingDetail(telemetry);
                    } else {
                        platform.log("Stop level reached (" + source + ") at "
                                + telemetry.batteryPercent + "%");
                        enable();
                    }
                    return;
                case ACTIVE:
                    monitorActive(telemetry);
                    return;
                default:
                    return;
            }
        } catch (Throwable error) {
            platform.log("Monitoring failed: " + message(error));
            retryAfterFailure("Telemetry error: " + message(error));
        }
    }

    void disable(String reason, boolean stop) {
        if (stopping && stop) return;
        stopping = true;
        setWakeLock(false);
        try {
            restoreAndConfirm();
            limitReached = false;
            retryAttempts = 0;
            transition(State.OFF, reason);
        } catch (Throwable error) {
            transition(State.FAILED, "Critical restore failure: " + message(error));
        } finally {
            if (stop) platform.stopService(state == State.FAILED);
        }
    }

    /** Service teardown: never leave a half-applied or unmonitored separation behind. */
    void destroy() {
        platform.log("Service destroyed: state=" + state + ", desired=" + settings.desired());
        setWakeLock(false);
        if (state == State.ACTIVE || state == State.ENABLING) {
            disable("Service stopped", false);
        }
    }

    private void monitorActive(PowerTelemetry telemetry) throws Exception {
        if (!telemetry.usbPresent) {
            arm(telemetry);
            return;
        }
        if (telemetry.limit != telemetry.limitMax) {
            disable("Control changed externally", true);
            return;
        }
        if (settings.autoLimit()) {
            if (telemetry.batteryPercent <= settings.resumePercent()) limitReached = false;
            if (shouldChargeToLimit(telemetry)) {
                platform.log("Charge-again level reached at " + telemetry.batteryPercent + "%");
                enterLimitCharging(telemetry);
                return;
            }
        }

        if (telemetry.batteryCurrentUa < UNSAFE_BATTERY_CURRENT_UA) unsafeSamples++;
        else unsafeSamples = 0;
        if (unsafeSamples >= UNSAFE_SAMPLES_BEFORE_STOP) {
            platform.log("Safety stop: battery powering device at "
                    + telemetry.batteryCurrentUa + " uA while USB is present");
            settings.recordSafetyStop(SAFETY_STOP_REASON);
            disable(SAFETY_STOP_REASON, true);
            return;
        }
        String next = activeDetail(telemetry, true);
        if (!next.equals(detail)) transition(State.ACTIVE, next);
    }

    private void arm(PowerTelemetry telemetry) throws Exception {
        if (telemetry.limit != 0) restoreAndConfirm();
        else settings.setRequested(false);
        setWakeLock(false);
        transition(State.ARMED, NO_USB_DETAIL);
    }

    private void enterLimitCharging(PowerTelemetry telemetry) throws Exception {
        if (telemetry.limit != 0) restoreAndConfirm();
        limitReached = false;
        unsafeSamples = 0;
        retryAttempts = 0;
        setWakeLock(true);
        state = State.CHARGING_TO_LIMIT;
        updateLimitChargingDetail(telemetry);
    }

    private void updateLimitChargingDetail(PowerTelemetry telemetry) {
        String next = String.format(Locale.US, "Will stop at %d%% (now %d%%)",
                settings.limitPercent(), telemetry.batteryPercent);
        if (state != State.CHARGING_TO_LIMIT || !next.equals(detail)) {
            transition(State.CHARGING_TO_LIMIT, next);
        }
    }

    /**
     * A failure while bypass is wanted restores normal charging (the safe state) but keeps
     * the service alive and retries, instead of silently charging to 100% until reopened.
     */
    private void retryAfterFailure(String reason) {
        setWakeLock(false);
        try {
            restoreAndConfirm();
        } catch (Throwable restoreError) {
            transition(State.FAILED, "Critical restore failure: " + message(restoreError));
            stopping = true;
            platform.stopService(true);
            return;
        }
        limitReached = false;
        if (!settings.desired()) {
            stopping = true;
            transition(State.OFF, OFF_REASON);
            platform.stopService(false);
            return;
        }
        retryAttempts++;
        long delay = retryDelayMs(retryAttempts);
        nextRetryAt = platform.nowMillis() + delay;
        transition(State.RETRYING, String.format(Locale.US,
                "Charging normally; retry after at least %d s (%s)",
                delay / 1000L, reason));
    }

    private void failPermanently(String reason) {
        platform.log("Permanent failure: " + reason);
        stopping = true;
        setWakeLock(false);
        try {
            restoreAndConfirm();
            limitReached = false;
            transition(State.FAILED, reason);
        } catch (Throwable error) {
            transition(State.FAILED, "Critical restore failure: " + message(error));
        }
        platform.stopService(true);
    }

    private void restoreAndConfirm() throws Exception {
        hardware.writeLimit(0);
        String readback = hardware.readLimitRaw().trim();
        if (!"0".equals(readback)) {
            throw new IllegalStateException("Restore not confirmed: " + readback);
        }
        settings.setRequested(false);
    }

    private boolean shouldChargeToLimit(PowerTelemetry telemetry) {
        return ChargeLimitPolicy.shouldChargeToLimit(settings.autoLimit(),
                telemetry.batteryPercent, settings.limitPercent(), settings.resumePercent(),
                limitReached);
    }

    static boolean powerReady(PowerTelemetry telemetry) {
        return "Charging".equalsIgnoreCase(telemetry.batteryStatus)
                || "Full".equalsIgnoreCase(telemetry.batteryStatus)
                || "Not charging".equalsIgnoreCase(telemetry.batteryStatus);
    }

    private String activeDetail(PowerTelemetry telemetry, boolean withUsb) {
        if (settings.autoLimit()) {
            String base = String.format(Locale.US, "Stopped at %d%% - charges again at %d%%",
                    settings.limitPercent(), settings.resumePercent());
            return withUsb ? base + String.format(Locale.US, " - USB %.1f W",
                    telemetry.usbWatts()) : base;
        }
        return withUsb ? String.format(Locale.US, "Battery is not charging - USB %.1f W",
                telemetry.usbWatts()) : "Battery is not charging";
    }

    private void setWakeLock(boolean held) {
        if (wakeLockHeld == held) return;
        wakeLockHeld = held;
        platform.setChargeWakeLock(held);
        platform.log(held ? "Holding CPU awake until the stop level"
                : "Released charge-limit wake lock");
    }

    private void transition(State next, String nextDetail) {
        if (next != state) platform.log("State " + state + " -> " + next + ": " + nextDetail);
        state = next;
        detail = nextDetail;
        platform.publish(next, nextDetail);
    }

    private static String message(Throwable error) {
        return error.getMessage() == null ? error.toString() : error.getMessage();
    }
}
