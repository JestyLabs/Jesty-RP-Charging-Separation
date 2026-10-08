package com.jesty.rpchargingseparation;

import android.os.SystemClock;
import android.os.Process;
import android.system.Os;
import android.system.OsConstants;
import android.system.ErrnoException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileReader;

/** Marker-only research sentinel. No charging authority or lease renewal. */
public final class RestoreWatchdogSentinel {
    interface Clock { long elapsed(); void sleep() throws Exception; }
    interface Owner { long startTicks(int pid) throws Exception; }

    static String observe(int pid, long ticks, int seconds, Clock clock, Owner owner,
            ResearchRuntime runtime) throws Exception {
        long started = clock.elapsed();
        if (started < 0) throw new IllegalStateException("elapsed time unavailable");
        long previous = started;
        while (true) {
            long now = clock.elapsed();
            if (now < previous) throw new IllegalStateException("elapsed time regressed");
            previous = now;
            if (now - started >= seconds * 1000L) return "RESTORE_REQUIRED lease_expired";
            if (runtime.stopped()) return "SENTINEL_END stop_requested";
            long current = owner.startTicks(pid);
            if (current < 0) return "RESTORE_REQUIRED owner_missing";
            if (current != ticks) return "RESTORE_REQUIRED owner_mismatch";
            clock.sleep();
        }
    }

    public static void main(String[] args) {
        try (ResearchRuntime runtime = new ResearchRuntime("sentinel")) {
            if (args.length == 1) { runtime.control(args[0]); return; }
            if (args.length != 4 || !args[0].equals("run")) throw new IllegalArgumentException("arguments");
            int pid = Integer.parseInt(args[1]); long ticks = Long.parseLong(args[2]);
            if (pid <= 1 || ticks <= 0) throw new IllegalArgumentException("owner identity");
            int lease = RestoreWatchdogSentinelScript.clampLeaseSeconds(Integer.parseInt(args[3]));
            runtime.acquire();
            runtime.resetLog();
            LinuxProcessIdentity self = identity(Process.myPid());
            runtime.append("START sentinel pid=" + Process.myPid() + " start_ticks=" + self.startTimeTicks
                    + " owner_pid=" + pid + " owner_start_ticks=" + ticks + " lease_s=" + lease);
            runtime.append("IDENTITY uid=" + Process.myUid() + " classpath=" + System.getenv("CLASSPATH")
                    + " boot_id=" + bootId());
            try {
                runtime.append(observe(pid, ticks, lease, new Clock() {
                    public long elapsed() { return SystemClock.elapsedRealtime(); }
                    public void sleep() throws Exception { Thread.sleep(1000); }
                }, ownerPid -> {
                    try { return identity(ownerPid).startTimeTicks; }
                    catch (ErrnoException unavailable) {
                        if (unavailable.errno == OsConstants.ENOENT) return -1L;
                        throw unavailable;
                    }
                }, runtime));
            } catch (Exception failure) {
                runtime.append("SENTINEL_END observation_unavailable");
                throw failure;
            }
        } catch (Throwable failure) {
            System.err.println("sentinel failed: " + failure.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static String bootId() {
        try (BufferedReader in = new BufferedReader(new FileReader("/proc/sys/kernel/random/boot_id"))) {
            String line = in.readLine(); return line == null ? "?" : line.trim();
        } catch (Exception unavailable) { return "?"; }
    }

    private static LinuxProcessIdentity identity(int pid) throws Exception {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(
                Os.open("/proc/" + pid + "/stat", OsConstants.O_RDONLY
                        | OsConstants.O_CLOEXEC | OsConstants.O_NOFOLLOW | OsConstants.O_NONBLOCK, 0))))) {
            return LinuxProcessIdentity.parse(in.readLine());
        }
    }
}
