package com.jesty.rpchargingseparation;

final class ChargeLimitPolicy {
    static final int MIN_RESUME_PERCENT = 5;
    static final int MIN_RESUME_GAP = 5;

    private ChargeLimitPolicy() { }

    static int clampResumePercent(int resume, int limit) {
        return Math.max(MIN_RESUME_PERCENT, Math.min(resume, limit - MIN_RESUME_GAP));
    }

    static int migrateResumeMargin(int limit, int margin) {
        return clampResumePercent(limit - margin, limit);
    }

    static boolean shouldChargeToLimit(boolean automatic, int batteryPercent,
                                       int limitPercent, int resumePercent,
                                       boolean limitReached) {
        if (!automatic) return false;
        if (batteryPercent < 0 || batteryPercent > 100) {
            throw new IllegalArgumentException("Invalid battery percentage: " + batteryPercent);
        }
        if (batteryPercent >= limitPercent) return false;
        return !limitReached || batteryPercent <= resumePercent;
    }
}
