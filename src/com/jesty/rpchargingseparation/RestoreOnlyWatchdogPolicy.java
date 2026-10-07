package com.jesty.rpchargingseparation;

/**
 * Pure policy model for a possible future restore-only safety watchdog.
 *
 * This is research code: no Android dependencies, no sysfs access and no PServer calls.
 * The model deliberately has no action that can enable charging separation.
 */
final class RestoreOnlyWatchdogPolicy {
    enum Action { KEEP_WATCHING, RESTORE_NORMAL_THEN_EXIT, EXIT }

    private RestoreOnlyWatchdogPolicy() {}

    /**
     * @param ownerAlive exact owning Android process identity is still alive
     * @param leaseFresh owner has renewed its bounded watchdog lease recently
     * @param nativeSeparated native hardware currently reports separation active
     * @param retireRequested owner asked the watchdog to retire during a clean shutdown
     */
    static Action evaluate(boolean ownerAlive, boolean leaseFresh,
            boolean nativeSeparated, boolean retireRequested) {
        // A clean retire may only exit directly after native charging was already restored.
        if (retireRequested) {
            return nativeSeparated ? Action.RESTORE_NORMAL_THEN_EXIT : Action.EXIT;
        }

        // Lost owner or stale ownership is a fail-open event: normal charging wins.
        if (!ownerAlive || !leaseFresh) {
            return nativeSeparated ? Action.RESTORE_NORMAL_THEN_EXIT : Action.EXIT;
        }

        return Action.KEEP_WATCHING;
    }
}
