package com.jesty.rpchargingseparation;

import java.util.ArrayList;
import java.util.List;

/**
 * Scenario tests for the bypass state machine. They run on a plain JDK with a fake
 * Retroid power supply and a fake clock, so no device, root or Android SDK is needed.
 */
public final class BypassControllerTest {
    private static final int LIMIT_MAX = 1;

    /** Fake sysfs: battery/status follows the native limit like the Retroid firmware. */
    static final class FakeDevice implements BypassController.Hardware {
        int percent = 58;
        boolean usb;
        int limit;
        long batteryCurrentUa;
        String missing;
        String forcedStatus;
        int unsettledReads;
        int ignoredLimitWrites;
        int failingReads;
        boolean restoreBroken;
        int writes;

        String status() {
            if (forcedStatus != null) return forcedStatus;
            if (!usb) return "Discharging";
            if (unsettledReads > 0) {
                unsettledReads--;
                return "Unknown";
            }
            if (limit == LIMIT_MAX) return "Not charging";
            return percent >= 100 ? "Full" : "Charging";
        }

        @Override public String missingControls() { return missing; }

        @Override
        public PowerTelemetry read() throws Exception {
            if (failingReads > 0) {
                failingReads--;
                throw new java.io.IOException("sysfs read failed");
            }
            return new PowerTelemetry(batteryCurrentUa, 4_000_000L, usb ? 2_000_000L : 0L,
                    usb ? 5_000_000L : 0L, 0L, percent, 30f, usb, false,
                    usb ? "USB_PD" : "Unknown", status(), limit, LIMIT_MAX);
        }

        @Override
        public void writeLimit(int value) {
            writes++;
            if (value != 0 && ignoredLimitWrites > 0) {
                ignoredLimitWrites--;
                return;
            }
            if (value == 0 && restoreBroken) return;
            limit = value;
        }

        @Override public String readLimitRaw() { return limit + "\n"; }
    }

    static final class FakeSettings implements BypassController.Settings {
        boolean desired = true;
        boolean auto = true;
        int limitPercent = 80;
        int resumePercent = 70;
        boolean requested;
        String safetyStop;

        @Override public boolean desired() { return desired; }
        @Override public boolean autoLimit() { return auto; }
        @Override public int limitPercent() { return limitPercent; }
        @Override public int resumePercent() { return resumePercent; }
        @Override public void setRequested(boolean value) { requested = value; }

        @Override
        public void recordSafetyStop(String reason) {
            safetyStop = reason;
            desired = false;
        }
    }

    static final class FakePlatform implements BypassController.Platform {
        long now = 1_000L;
        boolean wakeLock;
        boolean stopped;
        boolean keptNotice;
        final List<String> log = new ArrayList<>();
        final List<BypassController.State> published = new ArrayList<>();

        @Override public long nowMillis() { return now; }
        @Override public void sleep(long millis) { now += millis; }
        @Override public void setChargeWakeLock(boolean held) { wakeLock = held; }
        @Override public void publish(BypassController.State state, String detail) {
            published.add(state);
        }

        @Override
        public void stopService(boolean keepNotice) {
            stopped = true;
            keptNotice = keepNotice;
        }

        @Override public void log(String message) { log.add(message); }
    }

    private static final class Rig {
        final FakeDevice device = new FakeDevice();
        final FakeSettings settings = new FakeSettings();
        final FakePlatform platform = new FakePlatform();
        final BypassController controller =
                new BypassController(device, settings, platform);

        BypassController.State state() { return controller.state(); }

        /** Only a battery event, as when the CPU sleeps and the poll timer is paused. */
        void event() {
            controller.tick("BATTERY_CHANGED");
            checkInvariants();
        }

        void checkInvariants() {
            boolean charging = state() == BypassController.State.CHARGING_TO_LIMIT;
            check(platform.wakeLock == charging, "wake lock must be held only while charging "
                    + "to the stop level; state=" + state() + ", held=" + platform.wakeLock);
            check(platform.wakeLock == controller.wakeLockHeld(), "wake lock bookkeeping");
            if (state() == BypassController.State.ARMED) {
                check(device.limit == 0, "ARMED must have normal charging restored");
            }
            if (state() != BypassController.State.ACTIVE && device.usb) {
                check(device.limit == 0 || device.restoreBroken,
                        "battery must charge normally outside ACTIVE; state=" + state());
            }
        }
    }

    private static int passed;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectState(Rig rig, BypassController.State expected) {
        check(rig.state() == expected, "expected " + expected + " but was " + rig.state()
                + " (" + rig.controller.detail() + ")");
        rig.checkInvariants();
    }

    /** Issue #2: bypass enabled before plugging in, then the device sleeps. */
    private static void armedThenPluggedWhileAsleepStopsAtLimit() {
        Rig rig = new Rig();
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.ARMED);

        rig.device.usb = true;
        rig.event();
        expectState(rig, BypassController.State.CHARGING_TO_LIMIT);
        check(rig.platform.wakeLock, "charging to the limit must keep the CPU awake");

        for (int percent = 59; percent < 80; percent++) {
            rig.device.percent = percent;
            rig.event();
            expectState(rig, BypassController.State.CHARGING_TO_LIMIT);
        }
        rig.device.percent = 80;
        rig.event();
        expectState(rig, BypassController.State.ACTIVE);
        check(rig.device.limit == LIMIT_MAX, "separation must be applied at exactly 80%");
        check(rig.settings.requested, "requested flag must be set while separated");
        passed++;
    }

    private static void hysteresisStopsAndResumes() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 80;
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.ACTIVE);

        for (int percent = 79; percent > 70; percent--) {
            rig.device.percent = percent;
            rig.event();
            expectState(rig, BypassController.State.ACTIVE);
        }
        rig.device.percent = 70;
        rig.event();
        expectState(rig, BypassController.State.CHARGING_TO_LIMIT);
        rig.device.percent = 75;
        rig.event();
        expectState(rig, BypassController.State.CHARGING_TO_LIMIT);
        rig.device.percent = 80;
        rig.event();
        expectState(rig, BypassController.State.ACTIVE);
        passed++;
    }

    private static void rightAwayModeSeparatesImmediately() {
        Rig rig = new Rig();
        rig.settings.auto = false;
        rig.device.usb = true;
        rig.device.percent = 40;
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.ACTIVE);
        rig.device.percent = 20;
        rig.event();
        expectState(rig, BypassController.State.ACTIVE);
        passed++;
    }

    private static void waitsForSlowCharger() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 85;
        rig.device.unsettledReads = 4;
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.ACTIVE);
        passed++;
    }

    /** Before the fix, a failed validation stopped the service and charged to 100%. */
    private static void transientValidationFailureRetries() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 85;
        rig.device.ignoredLimitWrites = 1;
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.RETRYING);
        check(!rig.platform.stopped, "a transient failure must not stop the service");
        check(rig.settings.desired, "a transient failure must not turn the user's choice off");
        check(rig.controller.retryAttempts() == 1, "first retry attempt");

        rig.event();
        expectState(rig, BypassController.State.RETRYING);
        rig.platform.now += BypassController.retryDelayMs(1);
        rig.event();
        expectState(rig, BypassController.State.ACTIVE);
        check(rig.controller.retryAttempts() == 0, "attempts reset after success");
        passed++;
    }

    private static void chargerThatNeverSettlesRetriesWithBackoff() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 85;
        rig.device.forcedStatus = "Unknown";
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.RETRYING);
        rig.platform.now += BypassController.retryDelayMs(1);
        rig.event();
        expectState(rig, BypassController.State.RETRYING);
        check(rig.controller.retryAttempts() == 2, "second attempt after the first delay");

        rig.device.forcedStatus = null;
        rig.platform.now += BypassController.retryDelayMs(2);
        rig.event();
        expectState(rig, BypassController.State.ACTIVE);
        passed++;
    }

    private static void telemetryErrorWhileChargingRetries() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 60;
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.CHARGING_TO_LIMIT);
        rig.device.failingReads = 1;
        rig.event();
        expectState(rig, BypassController.State.RETRYING);
        check(!rig.platform.stopped, "telemetry errors must not stop the service");

        rig.device.percent = 80;
        rig.platform.now += BypassController.retryDelayMs(1);
        rig.event();
        expectState(rig, BypassController.State.ACTIVE);
        passed++;
    }

    private static void unplugWhileRetryingArms() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 85;
        rig.device.ignoredLimitWrites = 1;
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.RETRYING);
        rig.device.usb = false;
        rig.event();
        expectState(rig, BypassController.State.ARMED);
        rig.device.usb = true;
        rig.event();
        expectState(rig, BypassController.State.ACTIVE);
        passed++;
    }

    private static void unplugAndReplugKeepsWorking() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 80;
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.ACTIVE);
        rig.device.usb = false;
        rig.device.percent = 60;
        rig.event();
        check(rig.state() == BypassController.State.ARMED, "unplug arms");
        rig.device.usb = true;
        rig.event();
        expectState(rig, BypassController.State.CHARGING_TO_LIMIT);
        rig.device.percent = 80;
        rig.event();
        expectState(rig, BypassController.State.ACTIVE);
        passed++;
    }

    /** An unplugged ARMED service must not leave native separation behind. */
    private static void unplugActiveRestoresBeforeArming() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 80;
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.ACTIVE);
        check(rig.device.limit == LIMIT_MAX && rig.settings.requested,
                "test starts with native separation active");

        rig.device.usb = false;
        rig.event();
        expectState(rig, BypassController.State.ARMED);
        check(!rig.settings.requested && rig.settings.desired,
                "unplug clears the native request but preserves the user's choice");
        check(!rig.platform.stopped, "service keeps watching for the next charger");

        rig.controller.destroy();
        check(rig.device.limit == 0, "service teardown cannot leave a stale limit");
        passed++;
    }

    private static void unplugRestoreFailureIsNotReportedAsReady() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 80;
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.ACTIVE);

        rig.device.restoreBroken = true;
        rig.device.usb = false;
        rig.event();
        expectState(rig, BypassController.State.FAILED);
        check(rig.device.limit == LIMIT_MAX, "failed restore must be visible to the test");
        check(rig.controller.detail().startsWith("Critical restore failure"),
                "restore failure must not be shown as READY");
        check(rig.platform.keptNotice, "restore failure keeps a visible notice");
        passed++;
    }

    private static void unplugAfterNativeAutoRestoreClearsRequest() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 80;
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.ACTIVE);

        rig.device.usb = false;
        rig.device.limit = 0; // Some firmware may clear its own native limit on unplug.
        rig.event();
        expectState(rig, BypassController.State.ARMED);
        check(!rig.settings.requested && rig.settings.desired,
                "native auto-restore still clears the recorded hardware request");
        passed++;
    }

    /** After a process restart the native setting shows the limit was already reached. */
    private static void restartAdoptsExistingSeparation() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 75;
        rig.device.limit = LIMIT_MAX;
        rig.controller.start("sticky restart");
        expectState(rig, BypassController.State.ACTIVE);
        check(rig.device.writes == 0, "adopting must not rewrite the native limit");
        rig.device.percent = 71;
        rig.event();
        expectState(rig, BypassController.State.ACTIVE);
        passed++;
    }

    private static void userTurningOffRestoresCharging() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 90;
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.ACTIVE);
        rig.settings.desired = false;
        rig.event();
        expectState(rig, BypassController.State.OFF);
        check(rig.device.limit == 0 && !rig.settings.requested, "OFF restores charging");
        check(rig.platform.stopped && !rig.platform.keptNotice, "OFF ends the service");
        passed++;
    }

    private static void externalChangeTurnsOff() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 90;
        rig.controller.start("ENABLE");
        rig.device.limit = 0;
        rig.event();
        expectState(rig, BypassController.State.OFF);
        passed++;
    }

    private static void batteryDrainTriggersSafetyStop() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 90;
        rig.controller.start("ENABLE");
        rig.device.batteryCurrentUa = -500_000L;
        rig.event();
        rig.event();
        expectState(rig, BypassController.State.ACTIVE);
        rig.event();
        expectState(rig, BypassController.State.OFF);
        check(BypassController.SAFETY_STOP_REASON.equals(rig.settings.safetyStop),
                "safety stop is recorded");
        check(!rig.settings.desired, "safety stop turns bypass off");
        passed++;
    }

    private static void unsupportedDeviceFailsWithoutRetry() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.missing = "Unsupported on test: missing charge_control_limit";
        rig.controller.start("ENABLE");
        expectState(rig, BypassController.State.FAILED);
        check(rig.platform.stopped && rig.platform.keptNotice,
                "permanent failures keep the reason visible");
        passed++;
    }

    private static void brokenRestoreIsCritical() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 90;
        rig.controller.start("ENABLE");
        rig.device.restoreBroken = true;
        rig.settings.desired = false;
        rig.event();
        expectState(rig, BypassController.State.FAILED);
        check(rig.controller.detail().startsWith("Critical restore failure"),
                "critical restore failure is reported");
        check(rig.platform.keptNotice, "critical failure keeps the notification");
        passed++;
    }

    private static void destroyRestoresActiveSeparation() {
        Rig rig = new Rig();
        rig.device.usb = true;
        rig.device.percent = 90;
        rig.controller.start("ENABLE");
        rig.controller.destroy();
        check(rig.device.limit == 0, "service teardown restores normal charging");
        passed++;
    }

    private static void backoffIsBounded() {
        check(BypassController.retryDelayMs(1) == 15_000L, "first retry after 15 s");
        check(BypassController.retryDelayMs(2) == 30_000L, "then 30 s");
        check(BypassController.retryDelayMs(3) == 60_000L, "then 60 s");
        check(BypassController.retryDelayMs(6) == 300_000L, "capped at 5 min");
        check(BypassController.retryDelayMs(60) == 300_000L, "no overflow");
        passed++;
    }

    public static void main(String[] args) {
        armedThenPluggedWhileAsleepStopsAtLimit();
        hysteresisStopsAndResumes();
        rightAwayModeSeparatesImmediately();
        waitsForSlowCharger();
        transientValidationFailureRetries();
        chargerThatNeverSettlesRetriesWithBackoff();
        telemetryErrorWhileChargingRetries();
        unplugWhileRetryingArms();
        unplugAndReplugKeepsWorking();
        unplugActiveRestoresBeforeArming();
        unplugRestoreFailureIsNotReportedAsReady();
        unplugAfterNativeAutoRestoreClearsRequest();
        restartAdoptsExistingSeparation();
        userTurningOffRestoresCharging();
        externalChangeTurnsOff();
        batteryDrainTriggersSafetyStop();
        unsupportedDeviceFailsWithoutRetry();
        brokenRestoreIsCritical();
        destroyRestoresActiveSeparation();
        backoffIsBounded();
        System.out.println("Bypass controller tests passed: " + passed);
    }
}
