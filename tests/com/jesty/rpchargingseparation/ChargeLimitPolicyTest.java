package com.jesty.rpchargingseparation;

public final class ChargeLimitPolicyTest {
    private static void expect(boolean expected, boolean automatic, int percent,
                               int limit, int resume, boolean reached) {
        boolean actual = ChargeLimitPolicy.shouldChargeToLimit(
                automatic, percent, limit, resume, reached);
        if (actual != expected) {
            throw new AssertionError("percent=" + percent + ", limit=" + limit
                    + ", resume=" + resume + ", reached=" + reached
                    + ": expected " + expected + ", got " + actual);
        }
    }

    public static void main(String[] args) {
        if (ChargeLimitPolicy.migrateResumeMargin(80, 10) != 70
                || ChargeLimitPolicy.migrateResumeMargin(30, 20) != 10
                || ChargeLimitPolicy.migrateResumeMargin(80, 1) != 75
                || ChargeLimitPolicy.clampResumePercent(79, 80) != 75) {
            throw new AssertionError("Resume-level migration or minimum gap is wrong");
        }
        expect(false, false, 50, 80, 70, false);
        expect(true, true, 69, 80, 70, false);
        expect(true, true, 70, 80, 70, false);
        expect(true, true, 79, 80, 70, false);
        expect(false, true, 80, 80, 70, false);
        expect(false, true, 79, 80, 70, true);
        expect(true, true, 70, 80, 70, true);
        expect(true, true, 69, 80, 70, true);
        expect(false, true, 100, 80, 70, true);
        expect(false, true, 89, 90, 80, true);
        expect(true, true, 80, 90, 80, true);
        for (int invalid : new int[] { -1, 101 }) {
            try {
                ChargeLimitPolicy.shouldChargeToLimit(true, invalid, 80, 70, false);
                throw new AssertionError("Accepted invalid battery percentage " + invalid);
            } catch (IllegalArgumentException expected) {
                // An invalid reading must not enable separation.
            }
        }
        System.out.println("ChargeLimitPolicyTest passed");
    }
}
