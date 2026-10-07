package com.jesty.rpchargingseparation;

/**
 * Pure command/script builder for the PServer process-survival research probe.
 *
 * This class deliberately knows nothing about charging sysfs. The probe exists only to
 * answer one question: can a bounded root app_process launched through PServerBinder stay
 * alive when Retroid kills the normal Android app process?
 */
final class ProcessSurvivalProbeCommand {
    static final String PACKAGE_NAME = "com.jesty.rpchargingseparation";
    static final String PROBE_CLASS = PACKAGE_NAME + ".ProcessSurvivalProbe";
    static final String PREFIX = "/data/local/tmp/jesty-rp-process-survival";
    static final String LOG_PATH = PREFIX + ".log";
    static final String STOP_PATH = PREFIX + ".stop";
    static final String LAUNCHER_PATH = PREFIX + "-launch.sh";
    static final int MIN_DURATION_SECONDS = 60;
    static final int DEFAULT_DURATION_SECONDS = 600;
    static final int MAX_DURATION_SECONDS = 1800;
    static final int MAX_PSERVER_COMMAND_CHARS = 255;

    private ProcessSurvivalProbeCommand() {}

    static int clampDurationSeconds(int seconds) {
        return Math.max(MIN_DURATION_SECONDS, Math.min(MAX_DURATION_SECONDS, seconds));
    }

    /**
     * PServer receives only a tiny, operator-free command. The actual background launch lives
     * in a script staged by the adb harness. This avoids depending on vendor parsing of long
     * inline commands, redirections or '&'.
     */
    static String launchCommand() {
        String command = "sh " + LAUNCHER_PATH;
        if (command.length() > MAX_PSERVER_COMMAND_CHARS) {
            throw new IllegalStateException("Probe launch command too long: " + command.length());
        }
        return command;
    }

    /**
     * Script staged under /data/local/tmp by the adb harness, then executed as root by PServer.
     * No setsid: Thor-side reverse engineering found app_process can fail under setsid on this
     * vendor stack. The short sleep gives the background VM time to detach before pservice
     * closes its launcher shell.
     */
    static String launcherScript(int requestedSeconds) {
        int seconds = clampDurationSeconds(requestedSeconds);
        return "#!/system/bin/sh\n"
                + "rm -f " + STOP_PATH + "\n"
                + "A=$(pm path " + PACKAGE_NAME + " | head -n 1)\n"
                + "A=${A#package:}\n"
                + "[ -n \"$A\" ] || exit 20\n"
                + "CLASSPATH=\"$A\" nohup app_process / " + PROBE_CLASS + " " + seconds
                + " </dev/null >/dev/null 2>&1 &\n"
                + "P=$!\n"
                + "sleep 1\n"
                + "kill -0 \"$P\" 2>/dev/null || exit 21\n"
                + "exit 0\n";
    }

    static String status() {
        return "tail -n 80 " + LOG_PATH + " 2>/dev/null";
    }

    static String stop() {
        return "touch " + STOP_PATH;
    }

    static String clean() {
        return "rm -f " + STOP_PATH + " " + LOG_PATH + " " + LAUNCHER_PATH;
    }
}
