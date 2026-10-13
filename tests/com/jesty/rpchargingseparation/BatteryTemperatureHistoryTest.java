package com.jesty.rpchargingseparation;

import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/** In-memory Android preferences fixture: persistence, reset and concurrent samples. */
public final class BatteryTemperatureHistoryTest {
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }

    private static final class MemoryPreferences implements SharedPreferences {
        final Map<String, Integer> values;
        int applyCount;

        MemoryPreferences() { this(new HashMap<String, Integer>()); }
        MemoryPreferences(Map<String, Integer> values) { this.values = values; }

        @Override
        public int getInt(String key, int fallback) {
            synchronized (values) {
                Integer value = values.get(key);
                return value == null ? fallback : value;
            }
        }

        @Override
        public Editor edit() {
            return new Editor() {
                final Map<String, Integer> updates = new HashMap<>();
                final Set<String> removals = new HashSet<>();
                @Override public Editor putInt(String key, int value) {
                    updates.put(key, value);
                    removals.remove(key);
                    return this;
                }
                @Override public Editor remove(String key) {
                    updates.remove(key);
                    removals.add(key);
                    return this;
                }
                @Override public void apply() {
                    synchronized (values) {
                        for (String key : removals) values.remove(key);
                        values.putAll(updates);
                        applyCount++;
                    }
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        MemoryPreferences prefs = new MemoryPreferences();
        prefs.values.put("unrelated_charge_limit", 80);
        check(!BatteryTemperatureHistory.read(prefs).hasSamples(), "no initial history");
        check(!BatteryTemperatureHistory.record(prefs, Float.NaN).hasSamples(),
                "ignore invalid sensor value");
        check(prefs.applyCount == 0, "invalid sample requires no write");
        BatteryTemperatureHistory.record(prefs, 30.7f);
        check(prefs.applyCount == 1, "first sample saved once");
        BatteryTemperatureHistory.record(prefs, 30.7f);
        BatteryTemperatureHistory.record(prefs, 30.6f);
        BatteryTemperatureHistory.record(prefs, 32.9f);
        check(prefs.applyCount == 3, "only first, new low and new high are saved");
        BatteryTemperatureExtremes e = BatteryTemperatureHistory.read(
                new MemoryPreferences(prefs.values));
        check(e.minTenths == 306 && e.maxTenths == 329, "records survive preferences reload");
        check(prefs.values.get("unrelated_charge_limit") == 80,
                "charge-mode preferences untouched");

        final CountDownLatch go = new CountDownLatch(1);
        Thread low = new Thread(() -> {
            try { go.await(); } catch (InterruptedException ex) { throw new RuntimeException(ex); }
            for (int i = 0; i < 50; i++) BatteryTemperatureHistory.record(prefs, 25.0f);
        });
        Thread high = new Thread(() -> {
            try { go.await(); } catch (InterruptedException ex) { throw new RuntimeException(ex); }
            for (int i = 0; i < 50; i++) BatteryTemperatureHistory.record(prefs, 41.1f);
        });
        low.start(); high.start(); go.countDown(); low.join(); high.join();
        e = BatteryTemperatureHistory.read(prefs);
        check(e.minTenths == 250 && e.maxTenths == 411,
                "activity/service concurrent reads preserve both bounds");
        BatteryTemperatureHistory.reset(prefs);
        check(!BatteryTemperatureHistory.read(prefs).hasSamples(), "reset clears records");
        check(prefs.values.get("unrelated_charge_limit") == 80,
                "reset leaves charge settings untouched");
        e = BatteryTemperatureHistory.record(prefs, 27.3f);
        check(e.minTenths == 273 && e.maxTenths == 273,
                "new period starts from next reading");
        System.out.println("Battery temperature persistence and concurrency tests passed");
    }
}
