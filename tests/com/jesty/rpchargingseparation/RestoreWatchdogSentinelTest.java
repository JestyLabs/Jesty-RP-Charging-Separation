package com.jesty.rpchargingseparation;

import android.system.Os;

public final class RestoreWatchdogSentinelTest {
    static final class Clock implements RestoreWatchdogSentinel.Clock {
        long elapsed, wall; int sleeps; boolean regress, jump;
        public long elapsed() { return elapsed; }
        public void sleep() {
            sleeps++; elapsed += regress ? -1 : jump ? 60000 : 1000;
            wall += sleeps % 2 == 0 ? -9000000 : 9000000;
        }
    }
    public static void main(String[] args) throws Exception {
        Os.reset();
        try (ResearchRuntime runtime = new ResearchRuntime("sentinel")) {
            runtime.acquire(); runtime.resetLog();
            Clock clock = new Clock();
            String result = RestoreWatchdogSentinel.observe(123, 456, 5, clock, pid -> 456, runtime);
            ResearchRuntimeTest.check(result.endsWith("lease_expired") && clock.sleeps == 5,
                    "wall clock jumps changed elapsed lease");
            clock = new Clock(); clock.jump = true;
            result = RestoreWatchdogSentinel.observe(123, 456, 5, clock, pid -> 456, runtime);
            ResearchRuntimeTest.check(result.endsWith("lease_expired") && clock.sleeps == 1,
                    "suspend-like elapsed jump extended lease");
            Clock regressed = new Clock(); regressed.regress = true;
            ResearchRuntimeTest.rejected(() -> RestoreWatchdogSentinel.observe(123, 456, 5,
                    regressed, pid -> 456, runtime));
            Clock unavailable = new Clock(); unavailable.elapsed = -1;
            ResearchRuntimeTest.rejected(() -> RestoreWatchdogSentinel.observe(123, 456, 5,
                    unavailable, pid -> 456, runtime));
            result = RestoreWatchdogSentinel.observe(123, 456, 5, new Clock(), pid -> -1, runtime);
            ResearchRuntimeTest.check(result.endsWith("owner_missing"), "owner missing");
            result = RestoreWatchdogSentinel.observe(123, 456, 5, new Clock(), pid -> 999, runtime);
            ResearchRuntimeTest.check(result.endsWith("owner_mismatch"), "PID reuse");
            ResearchRuntimeTest.rejected(() -> RestoreWatchdogSentinel.observe(123, 456, 5,
                    new Clock(), pid -> { throw new java.io.IOException("unavailable"); }, runtime));
            runtime.control("stop");
            result = RestoreWatchdogSentinel.observe(123, 456, 5, new Clock(), pid -> 456, runtime);
            ResearchRuntimeTest.check(result.equals("SENTINEL_END stop_requested"), "explicit stop");
        }
        System.out.println("Sentinel elapsed lease, clock failure, stop and owner identity regressions passed.");
    }
}
