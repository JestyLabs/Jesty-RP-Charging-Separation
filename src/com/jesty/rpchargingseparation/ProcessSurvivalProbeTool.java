package com.jesty.rpchargingseparation;

/**
 * adb/app_process entry point for process-resilience research.
 *
 * Script-print actions are local-only helpers for the adb harness. Mutating actions sent
 * through PServer are restricted to fixed research files under /data/local/tmp.
 */
public final class ProcessSurvivalProbeTool {
    private ProcessSurvivalProbeTool() {}

    public static void main(String[] args) {
        String action = args == null || args.length == 0 ? "status" : args[0];
        try {
            if ("print-launcher".equals(action)) {
                int duration = parseInt(args, 1,
                        ProcessSurvivalProbeCommand.DEFAULT_DURATION_SECONDS);
                System.out.print(ProcessSurvivalProbeCommand.launcherScript(duration));
                return;
            }
            if ("print-sentinel".equals(action)) {
                int pid = parseInt(args, 1, -1);
                long startTicks = parseLong(args, 2, -1L);
                int lease = parseInt(args, 3, 30);
                System.out.print(RestoreWatchdogSentinelScript.build(pid, startTicks, lease));
                return;
            }

            switch (action) {
                case "start":
                    submit(ProcessSurvivalProbeCommand.launchCommand(), "survival probe");
                    return;
                case "status":
                    print(RootBridge.exec(ProcessSurvivalProbeCommand.status()),
                            "no probe log");
                    return;
                case "stop":
                    submit(ProcessSurvivalProbeCommand.stop(), "stop marker");
                    return;
                case "clean":
                    submit(ProcessSurvivalProbeCommand.clean(), "probe cleanup");
                    return;
                case "sentinel-start":
                    submit(RestoreWatchdogSentinelScript.launchCommand(), "sentinel watchdog");
                    return;
                case "sentinel-status":
                    print(RootBridge.exec(RestoreWatchdogSentinelScript.statusCommand()),
                            "sentinel pending");
                    return;
                case "sentinel-stop":
                    submit(RestoreWatchdogSentinelScript.stopCommand(), "sentinel stop marker");
                    return;
                case "sentinel-clean":
                    submit(RestoreWatchdogSentinelScript.cleanCommand(), "sentinel cleanup");
                    return;
                default:
                    System.err.println("usage: print-launcher [60..1800] | start | status | "
                            + "stop | clean | print-sentinel <pid> <start_ticks> <lease_s> | "
                            + "sentinel-start | sentinel-status | sentinel-stop | sentinel-clean");
                    System.exit(2);
            }
        } catch (Throwable error) {
            System.err.println("probe tool failed: " + error.getClass().getSimpleName()
                    + ": " + error.getMessage());
            System.exit(1);
        }
    }

    private static void submit(String command, String label) throws Exception {
        RootBridge.exec(command);
        System.out.println(label + " submitted command_chars=" + command.length());
    }

    private static void print(String value, String empty) {
        System.out.print(value == null || value.trim().isEmpty() ? empty + "\n" : value);
    }

    private static int parseInt(String[] args, int index, int fallback) {
        if (args == null || args.length <= index) return fallback;
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long parseLong(String[] args, int index, long fallback) {
        if (args == null || args.length <= index) return fallback;
        try {
            return Long.parseLong(args[index]);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
