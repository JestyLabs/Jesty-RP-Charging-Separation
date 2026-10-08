package com.jesty.rpchargingseparation;

/** Pure retry policy for a future restore-only watchdog. */
final class RestoreRetryPolicy {
    private static final long[] DELAYS_MS = {
            1_000L, 2_000L, 5_000L, 10_000L, 30_000L, 60_000L
    };

    enum Decision { EXIT_VERIFIED_NORMAL, RETRY_RESTORE }

    private RestoreRetryPolicy() {}

    static Decision afterReadback(boolean normalChargingVerified) {
        return normalChargingVerified ? Decision.EXIT_VERIFIED_NORMAL : Decision.RETRY_RESTORE;
    }

    static long delayMs(int failedAttempts) {
        if (failedAttempts <= 0) return DELAYS_MS[0];
        int index = Math.min(failedAttempts - 1, DELAYS_MS.length - 1);
        return DELAYS_MS[index];
    }

    /**
     * Once owner loss requires a restore, the original watchdog lease no longer authorizes
     * exiting. A restore-only helper is safer sleeping and retrying at the capped cadence than
     * abandoning a known unmonitored separation. Reboot still terminates the process.
     */
    static boolean mayExitWithoutVerifiedNormal() {
        return false;
    }
}
