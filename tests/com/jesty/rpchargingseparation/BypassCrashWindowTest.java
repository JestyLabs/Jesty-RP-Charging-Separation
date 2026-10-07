package com.jesty.rpchargingseparation;

import java.util.ArrayList;
import java.util.List;

/**
 * Research characterization of current process-death windows.
 * These tests intentionally describe current behavior; they do not change policy.
 */
public final class BypassCrashWindowTest {
    private static final int LIMIT_MAX = 10;
    private static int passed;

    static final class Device implements BypassController.Hardware {
        boolean usb = true;
        int percent = 80;
        int limit;
        int writes;

        @Override public String missingControls() { return null; }

        @Override
        public PowerTelemetry read() {
            String status;
            if (!usb) status = "Discharging";
            else if (limit == LIMIT_MAX) status = "Not charging";
            else status = "Charging";
            return new PowerTelemetry(0L, 4_000_000L,
                    usb ? 2_000_000L : 0L, usb ? 5_000_000L : 0L,
                    0L, percent, 30f, usb, false,
                    usb ? "USB_PD" : "Unknown", status, limit, LIMIT_MAX);
        }

        @Override
        public void writeLimit(int value) {
            writes++;
            limit = value;
        }

        @Override public String readLimitRaw() { return limit + "\n"; }
    }

    static final class Settings implements BypassController.Settings {
        boolean desired = true;
        boolean requested;

        @Override public boolean desired() { return desired; }
        @Override public boolean autoLimit() { return true; }
        @Override public int limitPercent() { return 80; }
        @Override public int resumePercent() { return 70; }
        @Override public void setRequested(boolean value) { requested = value; }
        @Override public void recordSafetyStop(String reason) { desired = false; }
    }

    static final class Platform implements BypassController.Platform {
        long now = 1000L;
        final List<String> log = new ArrayList<>();

        @Override public long nowMillis() { return now; }
        @Override public void sleep(long millis) { now += millis; }
        @Override public void setChargeWakeLock(boolean held) {}
        @Override public void publish(BypassController.State state, String detail) {}
        @Override public void stopService(boolean keepNotice) {}
        @Override public void log(String message) { log.add(message); }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static BypassController active(Device device, Settings settings, Platform platform) {
        BypassController controller = new BypassController(device, settings, platform);
        controller.start("research");
        check(controller.state() == BypassController.State.ACTIVE, "must start ACTIVE");
        check(device.limit == LIMIT_MAX, "ACTIVE must apply native separation");
        return controller;
    }

    private static void activeUnplugTransitionsToArmedWithoutRestore() {
        Device device = new Device();
        Settings settings = new Settings();
        Platform platform = new Platform();
        BypassController controller = active(device, settings, platform);

        int writesBefore = device.writes;
        device.usb = false;
        controller.tick("unplug");

        check(controller.state() == BypassController.State.ARMED, "unplug must arm");
        check(device.limit == LIMIT_MAX,
                "current ACTIVE -> ARMED path intentionally leaves native limit non-zero");
        check(device.writes == writesBefore, "unplug path performs no restore write");
        passed++;
    }

    private static void destroyFromArmedDoesNotRestoreToday() {
        Device device = new Device();
        Settings settings = new Settings();
        Platform platform = new Platform();
        BypassController controller = active(device, settings, platform);

        device.usb = false;
        controller.tick("unplug");
        int writesBeforeDestroy = device.writes;
        controller.destroy();

        check(controller.state() == BypassController.State.ARMED,
                "current destroy() leaves ARMED state unchanged");
        check(device.limit == LIMIT_MAX,
                "current destroy() does not restore an ARMED non-zero native limit");
        check(device.writes == writesBeforeDestroy,
                "destroy from ARMED performs no hardware write");
        passed++;
    }

    private static void stickyRestartWhileUnpluggedRepairsThePersistedLimit() {
        Device device = new Device();
        Settings settings = new Settings();
        Platform firstPlatform = new Platform();
        BypassController first = active(device, settings, firstPlatform);

        device.usb = false;
        first.tick("unplug");
        check(device.limit == LIMIT_MAX, "precondition: non-zero limit persists");

        // Abrupt process death: do not call destroy(). A new process/controller starts later.
        Platform restartedPlatform = new Platform();
        BypassController restarted = new BypassController(device, settings, restartedPlatform);
        restarted.start("sticky restart");

        check(restarted.state() == BypassController.State.ARMED,
                "restart while unplugged returns to ARMED");
        check(device.limit == 0,
                "restart reconciliation restores normal charging while unplugged");
        check(!settings.requested, "restart restore clears requested marker");
        passed++;
    }

    private static void explicitOffFromArmedRestores() {
        Device device = new Device();
        Settings settings = new Settings();
        Platform platform = new Platform();
        BypassController controller = active(device, settings, platform);

        device.usb = false;
        controller.tick("unplug");
        settings.desired = false;
        controller.tick("off");

        check(controller.state() == BypassController.State.OFF, "explicit OFF reaches OFF");
        check(device.limit == 0, "explicit OFF restores native limit");
        passed++;
    }

    public static void main(String[] args) {
        activeUnplugTransitionsToArmedWithoutRestore();
        destroyFromArmedDoesNotRestoreToday();
        stickyRestartWhileUnpluggedRepairsThePersistedLimit();
        explicitOffFromArmedRestores();
        System.out.println("Bypass crash-window characterization tests passed: " + passed);
    }
}
