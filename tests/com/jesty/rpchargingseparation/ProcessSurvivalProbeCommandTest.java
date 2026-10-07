package com.jesty.rpchargingseparation;

public final class ProcessSurvivalProbeCommandTest {
    private static int passed;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void launchIsBoundedAndChargingBlind() {
        String command = ProcessSurvivalProbeCommand.launch(
                ProcessSurvivalProbeCommand.MAX_DURATION_SECONDS);
        check(command.length() <= ProcessSurvivalProbeCommand.MAX_PSERVER_COMMAND_CHARS,
                "launch command exceeds PServer ceiling: " + command.length());
        check(command.contains("app_process / "
                        + ProcessSurvivalProbeCommand.PROBE_CLASS),
                "launch must target the fixed probe class");
        check(command.contains("nohup"), "probe must detach stdio");
        check(command.contains("setsid"), "probe should use a new session when available");
        check(!command.contains("charge_control"), "probe must not touch charge controls");
        check(!command.contains("/sys/class/power_supply"),
                "probe must not touch power-supply sysfs");
        check(!command.contains("settings put"), "probe must not mutate Settings");
        passed++;
    }

    private static void durationIsClamped() {
        String low = ProcessSurvivalProbeCommand.launch(-1);
        String high = ProcessSurvivalProbeCommand.launch(Integer.MAX_VALUE);
        check(low.contains(" " + ProcessSurvivalProbeCommand.MIN_DURATION_SECONDS
                + " </dev/null"), "minimum duration clamp");
        check(high.contains(" " + ProcessSurvivalProbeCommand.MAX_DURATION_SECONDS
                + " </dev/null"), "maximum duration clamp");
        passed++;
    }

    private static void supportCommandsStayInTempOnly() {
        for (String command : new String[]{
                ProcessSurvivalProbeCommand.status(),
                ProcessSurvivalProbeCommand.stop(),
                ProcessSurvivalProbeCommand.clean()}) {
            check(command.contains("/data/local/tmp/jesty-rp-process-survival"),
                    "support command escaped probe namespace: " + command);
            check(!command.contains("charge_control"), "support command touches charging");
            check(!command.contains("settings put"), "support command mutates Settings");
        }
        passed++;
    }

    public static void main(String[] args) {
        launchIsBoundedAndChargingBlind();
        durationIsClamped();
        supportCommandsStayInTempOnly();
        System.out.println("Process survival probe command tests passed: " + passed);
    }
}
