package com.jesty.rpchargingseparation;

public final class RestoreWatchdogSentinelScriptTest {
    private static int passed;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void pserverCommandsStayFixedAndChargingBlind() {
        check(RestoreWatchdogSentinelScript.statusCommand().contains("base64 -w 0"),
                "sentinel status must survive pservice first-line output");
        String launch = RestoreWatchdogSentinelScript.launchCommand();
        check(launch.equals("sh " + RestoreWatchdogSentinelScript.SCRIPT_PATH),
                "PServer launch must only execute fixed script");
        check(!launch.contains("&") && !launch.contains(">") && !launch.contains("setsid"),
                "launch must avoid inline shell operators and setsid");
        for (String command : new String[]{
                launch, RestoreWatchdogSentinelScript.statusCommand(),
                RestoreWatchdogSentinelScript.stopCommand(),
                RestoreWatchdogSentinelScript.cleanCommand()}) {
            check(command.contains(RestoreWatchdogSentinelScript.PREFIX),
                    "command escaped sentinel namespace: " + command);
            check(!command.contains("charge_control"), "command touches charging");
            check(!command.contains("/sys/class/power_supply"), "command touches power sysfs");
            check(!command.contains("settings put"), "command mutates settings");
        }
        passed++;
    }

    private static void scriptIsResearchOnly() {
        String script = RestoreWatchdogSentinelScript.build(123, 456789L, 30);
        check(script.contains("PID=123"), "pid pinned");
        check(script.contains("START=456789"), "starttime pinned");
        check(script.contains("CUR=${20}"), "field 22 must use positional parameter 20");
        check(script.contains("nohup sh \"$0\" worker"), "script self-detaches worker");
        check(script.contains("kill -0 \"$P\""), "launcher verifies worker survived grace");
        check(script.contains("SENTINEL_END stop_requested"), "explicit stop is observable");
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
        pserverCommandsStayFixedAndChargingBlind();
        scriptIsResearchOnly();
        leaseIsClamped();
        invalidIdentityFailsClosed();
        System.out.println("Restore watchdog sentinel script tests passed: " + passed);
    }
}
