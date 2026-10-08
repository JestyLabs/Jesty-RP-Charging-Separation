package com.jesty.rpchargingseparation;

public final class RestoreRetryPolicyTest {
    private static int passed;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void verifiedNormalIsOnlySuccessExit() {
        check(RestoreRetryPolicy.afterReadback(true)
                        == RestoreRetryPolicy.Decision.EXIT_VERIFIED_NORMAL,
                "verified normal charging exits");
        check(RestoreRetryPolicy.afterReadback(false)
                        == RestoreRetryPolicy.Decision.RETRY_RESTORE,
                "unverified restore retries");
        check(!RestoreRetryPolicy.mayExitWithoutVerifiedNormal(),
                "watchdog may not abandon unverified separation");
        passed++;
    }

    private static void backoffIsBounded() {
        long[] expected = {1000L, 1000L, 2000L, 5000L, 10000L, 30000L, 60000L, 60000L};
        for (int attempts = 0; attempts < expected.length; attempts++) {
            check(RestoreRetryPolicy.delayMs(attempts) == expected[attempts],
                    "attempt " + attempts + " delay");
        }
        check(RestoreRetryPolicy.delayMs(Integer.MAX_VALUE) == 60000L,
                "delay remains capped");
        passed++;
    }

    public static void main(String[] args) {
        verifiedNormalIsOnlySuccessExit();
        backoffIsBounded();
        System.out.println("Restore retry policy tests passed: " + passed);
    }
}
