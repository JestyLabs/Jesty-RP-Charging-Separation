package com.jesty.rpchargingseparation;

public final class RestoreWatchdogSentinelScriptTest {
    private static int passed;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void scriptIsResearchOnly() {
        String script = RestoreWatchdogSentinelScript.build(123, 456789L, 30);
        check(script.contains("PID=123"), "pid pinned");
        check(script.contains("START=456789"), "starttime pinned");
        check(script.contains("RESTORE_REQUIRED owner_missing"), "owner death marker");
        check(script.contains("RESTORE_REQUIRED owner_mismatch"), "pid reuse marker");
        check(script.contains("RESTORE_REQUIRED lease_expired"), "lease marker");
        check(!script.contains("charge_control"), "sentinel must not touch charging");
        check(!script.contains("/sys/class/power_supply"), "sentinel must not touch power sysfs");
        check(!script.contains("settings put"), "sentinel must not mutate settings");
        passed++;
    }

    private static void leaseIsClamped() {
        String low = RestoreWatchdogSentinelScript.build(2, 3L, -1);
        String high = RestoreWatchdogSentinelScript.build(2, 3L, Integer.MAX_VALUE);
        check(low.contains("+ " + RestoreWatchdogSentinelScript.MIN_LEASE_SECONDS + " )"),
                "minimum lease clamp");
        check(high.contains("+ " + RestoreWatchdogSentinelScript.MAX_LEASE_SECONDS + " )"),
                "maximum lease clamp");
        passed++;
    }

    private static void invalidIdentityFailsClosed() {
        try {
            RestoreWatchdogSentinelScript.build(1, 100L, 10);
            throw new AssertionError("pid 1 accepted");
        } catch (IllegalArgumentException expected) {}
        try {
            RestoreWatchdogSentinelScript.build(10, 0L, 10);
            throw new AssertionError("zero starttime accepted");
        } catch (IllegalArgumentException expected) {}
        passed++;
    }

    public static void main(String[] args) {
        scriptIsResearchOnly();
        leaseIsClamped();
        invalidIdentityFailsClosed();
        System.out.println("Restore watchdog sentinel script tests passed: " + passed);
    }
}
