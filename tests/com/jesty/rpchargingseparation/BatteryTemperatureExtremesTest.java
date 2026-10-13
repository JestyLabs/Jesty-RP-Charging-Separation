package com.jesty.rpchargingseparation;

/** JDK-only validation of invalid samples, monotonic bounds and reset semantics. */
public final class BatteryTemperatureExtremesTest {
    private static void check(boolean condition, String why) {
        if (!condition) throw new AssertionError(why);
    }

    public static void main(String[] args) {
        BatteryTemperatureExtremes empty = BatteryTemperatureExtremes.empty();
        check(!empty.hasSamples(), "no history until first valid reading");
        check(empty.observe(Float.NaN) == empty, "NaN is ignored");
        check(empty.observe(Float.POSITIVE_INFINITY) == empty, "infinity is ignored");
        check(empty.observe(-80) == empty, "bad negative sysfs reading");
        check(empty.observe(3276.7f) == empty, "bad high sysfs reading");

        BatteryTemperatureExtremes first = empty.observe(30.7f);
        check(first.hasSamples() && first.minTenths == 307 && first.maxTenths == 307,
                "first reading initializes both");
        check(first.observe(30.7f) == first, "no allocation/write for equal reading");
        check(first.observe(30.6f).minTenths == 306, "min decreases");
        check(first.observe(40.2f).maxTenths == 402, "max increases");
        BatteryTemperatureExtremes range = first.observe(30.6f).observe(40.2f);
        check(range.observe(35f) == range, "intermediate sample leaves record untouched");
        check(range.minC() == 30.6f && range.maxC() == 40.2f,
                "tenths conversion remains exact");
        check(range.observe(Float.NaN) == range, "invalid sample cannot replace range");
        check(!new BatteryTemperatureExtremes(500, 100).hasSamples(),
                "corrupted stored range is ignored");
        check(BatteryTemperatureExtremes.empty().observe(-10.3f).minTenths == -103,
                "cold sample is retained");
        check(BatteryTemperatureExtremes.empty().observe(80.3f).maxTenths == 803,
                "high but plausible sample retained");
        BatteryTemperatureExtremes reset = BatteryTemperatureExtremes.empty();
        check(reset.observe(21.4f).minTenths == 214, "new epoch after reset");
        System.out.println("Battery temperature bounds tests passed");
    }
}
