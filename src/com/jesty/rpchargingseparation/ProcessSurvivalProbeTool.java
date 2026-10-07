package com.jesty.rpchargingseparation;

/**
 * adb/app_process entry point for the research survival probe.
 *
 * The normal actions reach PServerBinder through the app's existing RootBridge contract.
 * "print-launcher" is intentionally local-only: the adb harness uses it to materialize
 * the exact launch script before asking PServer to run that script as root.
 */
public final class ProcessSurvivalProbeTool {
    private ProcessSurvivalProbeTool() {}

    public static void main(String[] args) {
        String action = args == null || args.length == 0 ? "status" : args[0];
        try {
            if ("print-launcher".equals(action)) {
                int duration = parseDuration(args, 1);
                System.out.print(ProcessSurvivalProbeCommand.launcherScript(duration));
                return;
            }

            switch (action) {
                case "start":
                    String command = ProcessSurvivalProbeCommand.launchCommand();
                    RootBridge.exec(command);
                    System.out.println("submitted command_chars=" + command.length());
                    return;
                case "status":
                    String status = RootBridge.exec(ProcessSurvivalProbeCommand.status());
                    System.out.print(status == null || status.isEmpty()
                            ? "no probe log\n" : status);
                    return;
                case "stop":
                    RootBridge.exec(ProcessSurvivalProbeCommand.stop());
                    System.out.println("stop marker submitted");
                    return;
                case "clean":
                    RootBridge.exec(ProcessSurvivalProbeCommand.clean());
                    System.out.println("probe files removed");
                    return;
                default:
                    System.err.println("usage: print-launcher [60..1800] | start | status | stop | clean");
                    System.exit(2);
            }
        } catch (Throwable error) {
            System.err.println("probe tool failed: " + error.getClass().getSimpleName()
                    + ": " + error.getMessage());
            System.exit(1);
        }
    }

    private static int parseDuration(String[] args, int index) {
        if (args == null || args.length <= index) {
            return ProcessSurvivalProbeCommand.DEFAULT_DURATION_SECONDS;
        }
        try {
            return ProcessSurvivalProbeCommand.clampDurationSeconds(Integer.parseInt(args[index]));
        } catch (NumberFormatException ignored) {
            return ProcessSurvivalProbeCommand.DEFAULT_DURATION_SECONDS;
        }
    }
}
