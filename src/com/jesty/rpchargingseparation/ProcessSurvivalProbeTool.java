package com.jesty.rpchargingseparation;

/**
 * adb/app_process entry point for the research survival probe.
 *
 * Example:
 *   CLASSPATH=<installed APK> app_process / \
 *     com.jesty.rpchargingseparation.ProcessSurvivalProbeTool start 600
 *
 * The tool reaches PServerBinder through the app's existing RootBridge contract.
 * It is intentionally not wired into the Android UI or manifest.
 */
public final class ProcessSurvivalProbeTool {
    private ProcessSurvivalProbeTool() {}

    public static void main(String[] args) {
        String action = args == null || args.length == 0 ? "status" : args[0];
        try {
            switch (action) {
                case "start":
                    int duration = ProcessSurvivalProbeCommand.DEFAULT_DURATION_SECONDS;
                    if (args.length > 1) {
                        try {
                            duration = Integer.parseInt(args[1]);
                        } catch (NumberFormatException ignored) {
                            duration = ProcessSurvivalProbeCommand.DEFAULT_DURATION_SECONDS;
                        }
                    }
                    String command = ProcessSurvivalProbeCommand.launch(duration);
                    RootBridge.exec(command);
                    System.out.println("submitted duration_s="
                            + ProcessSurvivalProbeCommand.clampDurationSeconds(duration)
                            + " command_chars=" + command.length());
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
                    System.err.println("usage: start [60..1800] | status | stop | clean");
                    System.exit(2);
            }
        } catch (Throwable error) {
            System.err.println("probe tool failed: " + error.getClass().getSimpleName()
                    + ": " + error.getMessage());
            System.exit(1);
        }
    }
}
