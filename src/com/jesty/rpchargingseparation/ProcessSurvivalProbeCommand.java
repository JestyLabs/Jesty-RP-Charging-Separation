package com.jesty.rpchargingseparation;

/** Fixed commands for the bounded, charging-blind research probe. */
final class ProcessSurvivalProbeCommand {
    static final String PACKAGE_NAME = "com.jesty.rpchargingseparation";
    static final String PROBE_CLASS = PACKAGE_NAME + ".ProcessSurvivalProbe";
    static final String LAUNCHER_PATH = "/data/local/tmp/jesty-rp-process-survival-launch.sh";
    static final int MIN_DURATION_SECONDS = 60;
    static final int DEFAULT_DURATION_SECONDS = 600;
    static final int MAX_DURATION_SECONDS = 1800;
    static final int MAX_PSERVER_COMMAND_CHARS = 255;
    static int clampDurationSeconds(int seconds) {
        return Math.max(MIN_DURATION_SECONDS, Math.min(MAX_DURATION_SECONDS, seconds));
    }
    static String launchCommand() { return "sh " + LAUNCHER_PATH; }
    static String launcherScript(String apk, int seconds) {
        return ResearchLauncher.build(apk, PROBE_CLASS, "" + clampDurationSeconds(seconds));
    }
    static String status() { return launchCommand() + " status"; }
    static String stop() { return launchCommand() + " stop"; }
    static String clean() { return launchCommand() + " clean"; }
}
