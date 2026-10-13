package com.jesty.rpchargingseparation;

/** Pure scheduling rules for foreground update checks; safe for host-side tests. */
final class UpdateCheckPolicy {
    static final long REFRESH_INTERVAL_MS = 60L * 60L * 1000L;
    static final long FAILURE_RETRY_MS = 5L * 60L * 1000L;

    private UpdateCheckPolicy() { }

    static boolean shouldCheck(long now, long lastSuccess, long lastAttempt, boolean manual) {
        if (manual) return true;
        // Clock corrections must not indefinitely suppress checks.
        if (now < lastSuccess || now < lastAttempt) return true;
        if (lastSuccess > 0 && now - lastSuccess < REFRESH_INTERVAL_MS) return false;
        return lastAttempt <= 0 || now - lastAttempt >= FAILURE_RETRY_MS;
    }
}
