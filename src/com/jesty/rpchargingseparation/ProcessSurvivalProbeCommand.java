package com.jesty.rpchargingseparation;

/**
 * Pure command builder for the PServer process-survival research probe.
 *
 * This class deliberately knows nothing about charging sysfs. The probe exists only to
 * answer one question: can a bounded root app_process launched through PServerBinder stay
 * alive when Retroid kills the normal Android app process?
 */
final class ProcessSurvivalProbeCommand {
    static final String PACKAGE_NAME = "com.jesty.rpchargingseparation";
    static final String PROBE_CLASS = PACKAGE_NAME + ".ProcessSurvivalProbe";
    static final String LOG_PATH = "/data/local/tmp/jesty-rp-process-survival.log";
    static final String STOP_PATH = "/data/local/tmp/jesty-rp-process-survival.stop";
    static final int MIN_DURATION_SECONDS = 60;
    static final int DEFAULT_DURATION_SECONDS = 600;
    static final int MAX_DURATION_SECONDS = 1800;
    static final int MAX_PSERVER_COMMAND_CHARS = 255;

    private ProcessSurvivalProbeCommand() {}

    static int clampDurationSeconds(int seconds) {
        return Math.max(MIN_DURATION_SECONDS, Math.min(MAX_DURATION_SECONDS, seconds));
    }

    static String launch(int requestedSeconds) {
        int seconds = clampDurationSeconds(requestedSeconds);
        // Keep this below the observed PServer command-size ceiling. The installed app is a
        // single base APK, matching the launch pattern already proven in the Thor project.
        String command = "rm -f " + STOP_PATH
                + ";A=$(pm path " + PACKAGE_NAME + ");A=${A#*:}"
                + ";S=$(command -v setsid)"
                + ";CLASSPATH=$A nohup $S app_process / " + PROBE_CLASS + " " + seconds
                + " </dev/null >/dev/null 2>&1 &";
        if (command.length() > MAX_PSERVER_COMMAND_CHARS) {
            throw new IllegalStateException("Probe launch command too long: " + command.length());
        }
        return command;
    }

    static String status() {
        return "tail -n 80 " + LOG_PATH + " 2>/dev/null";
    }

    static String stop() {
        return "touch " + STOP_PATH;
    }

    static String clean() {
        return "rm -f " + STOP_PATH + " " + LOG_PATH;
    }
}
