package com.jesty.rpchargingseparation;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

final class PowerTelemetry {
    static final String BASE = "/sys/class/power_supply/";
    static final String LIMIT = BASE + "battery/charge_control_limit";
    static final String LIMIT_MAX = BASE + "battery/charge_control_limit_max";
    static final String USB_PRESENT = BASE + "usb/present";
    static final String USB_PD = BASE + "usb/pd_active";

    final long batteryCurrentUa;
    final long batteryVoltageUv;
    final long usbCurrentUa;
    final long usbVoltageUv;
    final long chargeCounterUah;
    final int batteryPercent;
    final float temperatureC;
    final boolean usbPresent;
    final boolean pdActive;
    final String usbType;
    final String batteryStatus;
    final int limit;
    final int limitMax;

    // Package-private so tests can build readings without sysfs.
    PowerTelemetry(long batteryCurrentUa, long batteryVoltageUv,
                           long usbCurrentUa, long usbVoltageUv, long chargeCounterUah,
                           int batteryPercent, float temperatureC, boolean usbPresent,
                           boolean pdActive, String usbType, String batteryStatus,
                           int limit, int limitMax) {
        this.batteryCurrentUa = batteryCurrentUa;
        this.batteryVoltageUv = batteryVoltageUv;
        this.usbCurrentUa = usbCurrentUa;
        this.usbVoltageUv = usbVoltageUv;
        this.chargeCounterUah = chargeCounterUah;
        this.batteryPercent = batteryPercent;
        this.temperatureC = temperatureC;
        this.usbPresent = usbPresent;
        this.pdActive = pdActive;
        this.usbType = usbType;
        this.batteryStatus = batteryStatus;
        this.limit = limit;
        this.limitMax = limitMax;
    }

    static PowerTelemetry read() throws Exception {
        return new PowerTelemetry(
                number(BASE + "battery/current_now"),
                number(BASE + "battery/voltage_now"),
                number(BASE + "usb/input_current_now"),
                number(BASE + "usb/voltage_now"),
                number(BASE + "battery/charge_counter"),
                (int) number(BASE + "battery/capacity"),
                number(BASE + "battery/temp") / 10f,
                number(USB_PRESENT) == 1,
                number(USB_PD) == 1,
                text(BASE + "usb/real_type"),
                text(BASE + "battery/status"),
                (int) number(LIMIT),
                (int) number(LIMIT_MAX));
    }

    double usbWatts() {
        return (usbCurrentUa / 1_000_000d) * (usbVoltageUv / 1_000_000d);
    }

    double batteryWatts() {
        return (batteryCurrentUa / 1_000_000d) * (batteryVoltageUv / 1_000_000d);
    }

    boolean nativeIdleMode() {
        return usbPresent && limitMax > 0 && limit == limitMax
                && "Not charging".equalsIgnoreCase(batteryStatus);
    }

    long displayedBatteryCurrentUa() {
        // Fuel-gauge transients are not presented as sustained battery flow while
        // the charger reports its native idle mode.
        return nativeIdleMode() ? 0L : batteryCurrentUa;
    }

    double displayedBatteryWatts() {
        return (displayedBatteryCurrentUa() / 1_000_000d)
                * (batteryVoltageUv / 1_000_000d);
    }

    double estimatedDeviceWatts() {
        if (nativeIdleMode()) return Math.max(0d, usbWatts());
        return Math.max(0d, usbWatts() - Math.max(0d, batteryWatts()));
    }

    boolean separationConfirmed() {
        return nativeIdleMode() && Math.abs(batteryCurrentUa) <= 100_000L;
    }

    private static long number(String path) throws Exception {
        return Long.parseLong(text(path));
    }

    private static String text(String path) throws Exception {
        File file = new File(path);
        byte[] data = new byte[256];
        try (FileInputStream input = new FileInputStream(file)) {
            int count = input.read(data);
            if (count < 0) return "";
            return new String(data, 0, count, StandardCharsets.UTF_8).trim();
        }
    }
}
