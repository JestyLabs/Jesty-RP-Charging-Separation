package com.jesty.rpchargingseparation;

final class ChargeLimitPolicy {
    private ChargeLimitPolicy() { }

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
