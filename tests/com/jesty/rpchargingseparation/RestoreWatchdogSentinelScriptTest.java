package com.jesty.rpchargingseparation;

public final class RestoreWatchdogSentinelScriptTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        String apk = "/data/local/tmp/candidate.apk";
        String script = RestoreWatchdogSentinelScript.build(apk, 123, 456789L, 30);
        check(script.contains("export CLASSPATH='" + apk + "'"), "explicit research candidate");
        check(script.contains("RestoreWatchdogSentinel run 123 456789 30"), "exact owner identity and lease");
        check(!script.contains("date +") && !script.contains(" > /data/"), "no wall clock or privileged shell outputs");
        check(RestoreWatchdogSentinelScript.build(apk, 2, 3, -1).contains("run 2 3 5"), "minimum lease");
        check(RestoreWatchdogSentinelScript.build(apk, 2, 3, Integer.MAX_VALUE).contains("run 2 3 120"), "maximum lease");
        for (String command : new String[]{RestoreWatchdogSentinelScript.launchCommand(), RestoreWatchdogSentinelScript.statusCommand(), RestoreWatchdogSentinelScript.stopCommand(), RestoreWatchdogSentinelScript.cleanCommand()})
            check(command.length() <= 255 && command.startsWith("sh " + RestoreWatchdogSentinelScript.SCRIPT_PATH), "short fixed control");
        try { RestoreWatchdogSentinelScript.build(apk, 1, 3, 30); throw new AssertionError("pid 1"); }
        catch (IllegalArgumentException expected) { }
        try { RestoreWatchdogSentinelScript.build(apk, 2, 0, 30); throw new AssertionError("zero start ticks"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Sentinel candidate, owner identity and lease bounds regressions passed.");
    }
}
