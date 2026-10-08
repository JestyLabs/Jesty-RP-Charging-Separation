package com.jesty.rpchargingseparation;

/** Launches the bounded marker-only sentinel from an explicit staged research APK. */
final class RestoreWatchdogSentinelScript {
    static final String PREFIX = "/data/local/tmp/jesty-rp-watchdog-sentinel";
    static final String SCRIPT_PATH = PREFIX + ".sh";
    static final int MIN_LEASE_SECONDS = 5;
    static final int MAX_LEASE_SECONDS = 120;
    static int clampLeaseSeconds(int seconds) {
        return Math.max(MIN_LEASE_SECONDS, Math.min(MAX_LEASE_SECONDS, seconds));
    }
    static String launchCommand() { return "sh " + SCRIPT_PATH; }
    static String statusCommand() { return launchCommand() + " status"; }
    static String stopCommand() { return launchCommand() + " stop"; }
    static String cleanCommand() { return launchCommand() + " clean"; }
    static String build(String apk, int ownerPid, long ownerStartTicks, int seconds) {
        if (ownerPid <= 1 || ownerStartTicks <= 0) throw new IllegalArgumentException("owner identity");
        return ResearchLauncher.build(apk,
                "com.jesty.rpchargingseparation.RestoreWatchdogSentinel",
                ownerPid + " " + ownerStartTicks + " " + clampLeaseSeconds(seconds));
    }
}
