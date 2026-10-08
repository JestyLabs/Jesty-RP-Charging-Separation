package com.jesty.rpchargingseparation;

public final class ProcessSurvivalProbeCommandTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        String apk = "/data/local/tmp/session/candidate.apk";
        String low = ProcessSurvivalProbeCommand.launcherScript(apk, -1);
        String high = ProcessSurvivalProbeCommand.launcherScript(apk, Integer.MAX_VALUE);
        check(low.contains("run 60 </dev/null"), "minimum duration");
        check(high.contains("run 1800 </dev/null"), "maximum duration");
        check(high.contains("export CLASSPATH='" + apk + "'"), "candidate propagated to worker and controls");
        check(!high.contains("pm path"), "stable app must never be substituted");
        check(high.contains("exec app_process / " + ProcessSurvivalProbeCommand.PROBE_CLASS), "control entry point");
        check(high.contains("kill -0"), "bounded launch acknowledgement");
        check(!high.contains("setsid"), "unsupported detach mechanism");
        for (String command : new String[]{ ProcessSurvivalProbeCommand.launchCommand(),
                ProcessSurvivalProbeCommand.status(), ProcessSurvivalProbeCommand.stop(), ProcessSurvivalProbeCommand.clean() }) {
            check(command.length() <= 255 && command.startsWith("sh " + ProcessSurvivalProbeCommand.LAUNCHER_PATH), "short fixed command");
        }
        for (String invalid : new String[]{ "", "/data/app/stable.apk", "/data/local/tmp/../stable.apk", "/data/local/tmp/a.apk;id", "/data/local/tmp/.hidden.apk" }) {
            try { ProcessSurvivalProbeCommand.launcherScript(invalid, 60); throw new AssertionError("accepted " + invalid); }
            catch (IllegalArgumentException expected) { }
        }
        System.out.println("Probe launcher candidate, bounds and invalid-path regressions passed.");
    }
}
