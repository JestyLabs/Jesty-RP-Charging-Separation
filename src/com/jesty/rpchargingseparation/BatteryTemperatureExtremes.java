package com.jesty.rpchargingseparation;

/** Immutable temperature bounds, in tenths of a degree Celsius (sysfs precision). */
final class BatteryTemperatureExtremes {
    static final int EMPTY_MIN = Integer.MAX_VALUE;
    static final int EMPTY_MAX = Integer.MIN_VALUE;

    final int minTenths;
    final int maxTenths;

    BatteryTemperatureExtremes(int minTenths, int maxTenths) {
        if (minTenths > maxTenths
                && !(minTenths == EMPTY_MIN && maxTenths == EMPTY_MAX)) {
            this.minTenths = EMPTY_MIN;
            this.maxTenths = EMPTY_MAX;
        } else {
            this.minTenths = minTenths;
            this.maxTenths = maxTenths;
        }
    }

    static BatteryTemperatureExtremes empty() {
        return new BatteryTemperatureExtremes(EMPTY_MIN, EMPTY_MAX);
    }

    boolean hasSamples() {
        return minTenths != EMPTY_MIN && maxTenths != EMPTY_MAX;
    }

    float minC() {
        return minTenths / 10f;
    }

    float maxC() {
        return maxTenths / 10f;
    }

    BatteryTemperatureExtremes observe(float celsius) {
        // Reject invalid driver readings, not legitimate cold/hot events. These bounds
        // are purely diagnostic and never affect charging limits or safety controls.
        if (Float.isNaN(celsius) || Float.isInfinite(celsius)
                || celsius < -40f || celsius > 125f) return this;
        int tenths = Math.round(celsius * 10f);
        int minimum = Math.min(minTenths, tenths);
        int maximum = Math.max(maxTenths, tenths);
        if (minTenths == minimum && maxTenths == maximum) return this;
        return new BatteryTemperatureExtremes(minimum, maximum);
    }
}
