package com.jesty.rpchargingseparation;

public final class RestoreOnlyWatchdogPolicyTest {
    private static int passed;

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void healthyOwnerKeepsWatching() {
        check(RestoreOnlyWatchdogPolicy.evaluate(true, true, true, false)
                        == RestoreOnlyWatchdogPolicy.Action.KEEP_WATCHING,
                "healthy owner must retain active separation");
        check(RestoreOnlyWatchdogPolicy.evaluate(true, true, false, false)
                        == RestoreOnlyWatchdogPolicy.Action.KEEP_WATCHING,
                "healthy owner may still be charging to limit");
        passed++;
    }

    private static void ownerDeathFailsOpen() {
        check(RestoreOnlyWatchdogPolicy.evaluate(false, true, true, false)
                        == RestoreOnlyWatchdogPolicy.Action.RESTORE_NORMAL_THEN_EXIT,
                "owner death while separated must restore normal charging");
        check(RestoreOnlyWatchdogPolicy.evaluate(false, true, false, false)
                        == RestoreOnlyWatchdogPolicy.Action.EXIT,
                "owner death while already normal needs no write");
        passed++;
    }

    private static void staleLeaseFailsOpenEvenIfPidStillExists() {
        check(RestoreOnlyWatchdogPolicy.evaluate(true, false, true, false)
                        == RestoreOnlyWatchdogPolicy.Action.RESTORE_NORMAL_THEN_EXIT,
                "stale ownership while separated must restore");
        check(RestoreOnlyWatchdogPolicy.evaluate(true, false, false, false)
                        == RestoreOnlyWatchdogPolicy.Action.EXIT,
                "stale ownership while normal must exit");
        passed++;
    }

    private static void retireCannotAbandonActiveSeparation() {
        check(RestoreOnlyWatchdogPolicy.evaluate(true, true, true, true)
                        == RestoreOnlyWatchdogPolicy.Action.RESTORE_NORMAL_THEN_EXIT,
                "retire must not abandon active separation");
        check(RestoreOnlyWatchdogPolicy.evaluate(true, true, false, true)
                        == RestoreOnlyWatchdogPolicy.Action.EXIT,
                "retire after restore may exit directly");
        passed++;
    }

    private static void actionSurfaceCannotEnableSeparation() {
        for (RestoreOnlyWatchdogPolicy.Action action : RestoreOnlyWatchdogPolicy.Action.values()) {
            check(!action.name().contains("ENABLE") && !action.name().contains("SEPARATE"),
                    "watchdog action surface must remain restore-only: " + action);
        }
        passed++;
    }

    public static void main(String[] args) {
        healthyOwnerKeepsWatching();
        ownerDeathFailsOpen();
        staleLeaseFailsOpenEvenIfPidStillExists();
        retireCannotAbandonActiveSeparation();
        actionSurfaceCannotEnableSeparation();
        System.out.println("Restore-only watchdog policy tests passed: " + passed);
    }
}
