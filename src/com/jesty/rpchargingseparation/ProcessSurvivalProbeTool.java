package com.jesty.rpchargingseparation;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * adb/app_process entry point for process-resilience research.
 *
 * Script-print actions are local-only helpers for the adb harness. Mutating actions sent
 * through PServer use fixed staged launchers and root-private research output.
 */
public final class ProcessSurvivalProbeTool {
    private ProcessSurvivalProbeTool() {}

    public static void main(String[] args) {
        String action = args == null || args.length == 0 ? "status" : args[0];
        try {
            if ("print-launcher".equals(action)) {
                int duration = parseInt(args, 1,
                        ProcessSurvivalProbeCommand.DEFAULT_DURATION_SECONDS);
                System.out.print(ProcessSurvivalProbeCommand.launcherScript(requiredArg(args, 2), duration));
                return;
            }
            if ("print-sentinel".equals(action)) {
                int pid = parseInt(args, 1, -1);
                long startTicks = parseLong(args, 2, -1L);
                int lease = parseInt(args, 3, 30);
                System.out.print(RestoreWatchdogSentinelScript.build(requiredArg(args, 4), pid, startTicks, lease));
                return;
            }

            switch (action) {
                case "start":
                    submit(ProcessSurvivalProbeCommand.launchCommand(), "survival probe");
                    return;
                case "status":
                    print(decodeStatus(RootBridge.exec(ProcessSurvivalProbeCommand.status())),
                            "UNKNOWN: probe status unavailable");
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
                    print(decodeStatus(RootBridge.exec(RestoreWatchdogSentinelScript.statusCommand())),
                            "UNKNOWN: sentinel status unavailable");
                    return;
                case "sentinel-stop":
                    submit(RestoreWatchdogSentinelScript.stopCommand(), "sentinel stop marker");
                    return;
                case "sentinel-clean":
                    submit(RestoreWatchdogSentinelScript.cleanCommand(), "sentinel cleanup");
                    return;
                default:
                    System.err.println("usage: print-launcher <60..1800> <research.apk> | start | status | "
                            + "stop | clean | print-sentinel <pid> <start_ticks> <lease_s> <research.apk> | "
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
        String receipt = RootBridge.exec(command);
        if (receipt == null || !receipt.trim().equals("OK"))
            throw new IllegalStateException(label + " rejected or unavailable");
        System.out.println(label + " acknowledged; verify worker identity with Status");
    }

    private static void print(String value, String empty) {
        System.out.print(value == null || value.trim().isEmpty() ? empty + "\n" : value);
    }

    private static String decodeStatus(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) return "";
        return new String(Base64.getDecoder().decode(encoded.trim()), StandardCharsets.UTF_8);
    }

    private static String requiredArg(String[] args, int index) {
        if (args == null || args.length <= index) throw new IllegalArgumentException("research APK required");
        return args[index];
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
