package com.jesty.rpchargingseparation;

public final class ProcessSurvivalProbeCommandTest {
    private static int passed;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void binderCommandIsTinyAndOperatorFree() {
        String command = ProcessSurvivalProbeCommand.launchCommand();
        check(command.length() <= ProcessSurvivalProbeCommand.MAX_PSERVER_COMMAND_CHARS,
                "launch command exceeds PServer ceiling: " + command.length());
        check(command.equals("sh " + ProcessSurvivalProbeCommand.LAUNCHER_PATH),
                "binder command must only execute the staged script");
        check(!command.contains("&"), "binder command must not background inline");
        check(!command.contains(">"), "binder command must not redirect inline");
        check(!command.contains("setsid"), "binder command must not use setsid");
        passed++;
    }

    private static void launcherScriptIsBoundedAndChargingBlind() {
        String low = ProcessSurvivalProbeCommand.launcherScript(-1);
        String high = ProcessSurvivalProbeCommand.launcherScript(Integer.MAX_VALUE);
        check(low.contains(" " + ProcessSurvivalProbeCommand.MIN_DURATION_SECONDS
                + " </dev/null"), "minimum duration clamp");
        check(high.contains(" " + ProcessSurvivalProbeCommand.MAX_DURATION_SECONDS
                + " </dev/null"), "maximum duration clamp");
        check(high.contains("app_process / " + ProcessSurvivalProbeCommand.PROBE_CLASS),
                "launcher must target fixed probe class");
        check(high.contains("nohup"), "launcher must detach stdio");
        check(high.contains("sleep 1"), "launcher must keep parent alive briefly");
        check(!high.contains("setsid"), "launcher must avoid unproven setsid path");
        check(!high.contains("charge_control"), "probe must not touch charge controls");
        check(!high.contains("/sys/class/power_supply"),
                "probe must not touch power-supply sysfs");
        check(!high.contains("settings put"), "probe must not mutate Settings");
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
        binderCommandIsTinyAndOperatorFree();
        launcherScriptIsBoundedAndChargingBlind();
        supportCommandsStayInTempOnly();
        System.out.println("Process survival probe command tests passed: " + passed);
    }
}
