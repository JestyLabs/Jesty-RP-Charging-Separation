package com.jesty.rpchargingseparation;

import android.system.Os;
import android.system.StructStat;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

/** Runs the actual runtime against an Android syscall fixture and real host file locks. */
public final class ResearchRuntimeTest {
    interface Attempt { void run() throws Exception; }
    static void rejected(Attempt attempt) throws Exception {
        try { attempt.run(); } catch (Exception expected) { return; }
        throw new AssertionError("unsafe operation accepted");
    }
    static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    static final String DIR = "/data/jesty-rp-research-probe";
    static String log() throws Exception {
        return new String(Files.readAllBytes(Os.root.resolve(DIR.substring(1) + "/log")), StandardCharsets.UTF_8);
    }
    public static void main(String[] args) throws Exception {
        Os.reset(); Os.metadata.put("/data", new StructStat(0040777, 0));
        rejected(() -> new ResearchRuntime("probe"));
        Os.reset(); Os.metadata.get("/data").st_uid = 2000;
        rejected(() -> new ResearchRuntime("probe"));
        Os.reset(); Os.metadata.get("/data").st_gid = 2000;
        rejected(() -> new ResearchRuntime("probe"));
        for (StructStat bad : new StructStat[]{new StructStat(0040700, 2000),
                new StructStat(0040755, 0), new StructStat(0100700, 0)}) {
            Os.reset(); Os.metadata.put(DIR, bad);
            rejected(() -> new ResearchRuntime("probe"));
        }
        Os.reset(); Os.metadata.put(DIR, new StructStat(0040700, 0)); Os.links.add(DIR);
        rejected(() -> new ResearchRuntime("probe"));
        Os.reset();
        try (ResearchRuntime worker = new ResearchRuntime("probe")) {
            worker.acquire(); worker.resetLog(); worker.append("retained evidence");
            try (ResearchRuntime duplicate = new ResearchRuntime("probe")) {
                rejected(duplicate::acquire);
            }
            try (ResearchRuntime cleanup = new ResearchRuntime("probe")) {
                rejected(() -> cleanup.control("clean"));
            }
            check(log().equals("retained evidence\n"), "duplicate/cleanup damaged active evidence");
            try (ResearchRuntime stopper = new ResearchRuntime("probe")) { stopper.control("stop"); }
            check(worker.stopped(), "stop marker not observed");
        }
        try (ResearchRuntime cleanup = new ResearchRuntime("probe")) { cleanup.control("clean"); }
        check(!Files.exists(Os.root.resolve(DIR.substring(1) + "/log")), "terminal cleanup failed");
        check(Files.exists(Os.root.resolve(DIR.substring(1) + "/lock")), "lock inode must persist");
        try (ResearchRuntime next = new ResearchRuntime("probe")) { next.acquire(); next.resetLog(); }

        for (String name : new String[]{"lock", "log", "stop"}) {
            Os.reset();
            try (ResearchRuntime runtime = new ResearchRuntime("probe")) {
                runtime.resetLog(); runtime.control("stop");
                if (name.equals("lock")) { runtime.acquire(); runtime.close(); }
                Os.links.add(DIR + "/" + name);
                rejected(() -> {
                    if (name.equals("lock")) runtime.acquire();
                    else if (name.equals("log")) runtime.resetLog();
                    else runtime.stopped();
                });
                check(log().isEmpty(), "refused link modified log");
            }
        }
        for (StructStat bad : new StructStat[]{new StructStat(0100600, 2000),
                new StructStat(0100666, 0), new StructStat(0010600, 0)}) {
            Os.reset();
            try (ResearchRuntime runtime = new ResearchRuntime("probe")) {
                runtime.resetLog(); runtime.append("do not truncate");
                Os.metadata.put(DIR + "/log", bad);
                rejected(runtime::resetLog);
                check(log().equals("do not truncate\n"), "metadata validated after destructive open");
            }
        }
        Os.reset();
        try (ResearchRuntime runtime = new ResearchRuntime("probe")) {
            runtime.resetLog(); Os.metadata.get(DIR + "/log").st_nlink = 2;
            rejected(() -> runtime.append("unsafe hard link"));
        }
        System.out.println("Research runtime isolation, duplicate start, stop and cleanup regressions passed.");
    }
}
