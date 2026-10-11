package com.jesty.rpchargingseparation;

import android.content.FixtureState;
import android.os.Handler;
import android.service.quicksettings.Tile;

/** Actual tile/control lifecycle against host Android API fixtures; no hardware calls. */
public final class QuickSettingsTileTest {
    static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    static boolean enabled() { return FixtureState.prefs.getBoolean("desired_enabled", false); }
    public static void main(String[] args) {
        FixtureState.reset();
        ChargingSeparationTileService tile = new ChargingSeparationTileService();
        tile.onStartListening();
        check(FixtureState.starts == 0, "opening panel must not start hardware service");
        check(tile.tile.state == Tile.STATE_INACTIVE, "initial OFF");
        check(tile.tile.subtitle.equals("Off · unverified"), "cold process OFF is not hardware confirmation");
        int updates = tile.tile.updates;
        Handler.next.run(); Handler.next.run();
        check(tile.tile.updates == updates, "unchanged timer polls must not update the system tile");
        tile.locked = true; tile.onClick();
        check(!enabled() && FixtureState.starts == 0, "locked click changed charging");
        tile.locked = false; tile.unlock.run();
        check(enabled() && FixtureState.starts == 1, "unlocked click must request ON once");
        check(FixtureState.last.target == BypassService.class &&
                FixtureState.last.action.equals(BypassService.ACTION_ENABLE), "reuse existing service ENABLE");
        check(tile.tile.subtitle.equals("Enabled · open app"), "saved ON is not verified hardware ACTIVE");
        BypassService.current = BypassController.State.ARMED; Handler.next.run();
        check(tile.tile.subtitle.equals("Waiting for USB"), "armed state visible without preference change");
        BypassService.current = BypassController.State.CHARGING_TO_LIMIT; Handler.next.run();
        check(tile.tile.subtitle.equals("Charging to limit"), "automatic mode visible");
        BypassService.current = BypassController.State.ACTIVE; Handler.next.run();
        check(tile.tile.subtitle.equals("Separation active"), "controller-confirmed active");
        FixtureState.prefs.values.put("auto_on_boot", true);
        FixtureState.prefs.values.put("mode_fixture", true);
        FixtureState.prefs.values.put(BypassService.PREF_LAST_SAFETY_STOP, true);
        tile.onClick();
        check(!enabled() && FixtureState.starts == 2, "OFF dispatched while tile is a background service");
        check(FixtureState.last.action.equals(BypassService.ACTION_DISABLE), "reuse existing service DISABLE");
        check(!FixtureState.prefs.getBoolean("auto_on_boot", true), "OFF must disable auto-boot");
        check(FixtureState.prefs.getBoolean("mode_fixture", false), "mode settings must survive toggle");
        check(!FixtureState.prefs.values.containsKey(BypassService.PREF_LAST_SAFETY_STOP), "explicit action clears safety notice");
        check(tile.tile.subtitle.equals("Turning off"), "OFF choice must not report instant hardware restore");
        int calls = FixtureState.starts;
        tile.onStopListening(); tile.onDestroy();
        check(FixtureState.starts == calls && FixtureState.prefs.listeners.isEmpty() && Handler.next == null,
                "stop listening/destroy must remove observers, not alter charging");

        FixtureState.reset(); BypassService.current = BypassController.State.OFF;
        tile = new ChargingSeparationTileService(); tile.onStartListening();
        FixtureState.prefs.values.put("auto_on_boot", true);
        FixtureState.prefs.values.put(BypassService.PREF_LAST_SAFETY_STOP, "previous notice");
        FixtureState.prefs.commitOk = false; tile.onClick();
        check(!enabled() && FixtureState.starts == 0, "failed persistence must not dispatch ON");
        check(FixtureState.prefs.getBoolean("auto_on_boot", false) &&
                "previous notice".equals(FixtureState.prefs.values.get(BypassService.PREF_LAST_SAFETY_STOP)),
                "failed persistence must preserve previous settings and notice");
        FixtureState.prefs.commitOk = true; FixtureState.failStart = true; tile.onClick();
        check(enabled() && tile.tile.subtitle.equals("Start failed · open app"), "failed dispatch is not success");
        FixtureState.failStart = false; tile.onClick();
        check(!enabled() && !FixtureState.prefs.getBoolean(SeparationControl.PREF_START_ERROR, false),
                "next explicit action clears dispatch error");
        // Dashboard writes are visible without recreating the tile.
        FixtureState.prefs.edit().putBoolean(BypassService.PREF_DESIRED, true).apply();
        check(tile.tile.state == Tile.STATE_ACTIVE, "dashboard choice did not refresh tile");
        tile.onStopListening();
        android.os.Build.VERSION.SDK_INT = 33;

        // OFF request fails to dispatch while native separation is still active.
        FixtureState.reset(); FixtureState.prefs.values.put(BypassService.PREF_DESIRED, true);
        BypassService.current = BypassController.State.ACTIVE; BypassService.published = true;
        BypassControllerTest.FakeDevice hardware = new BypassControllerTest.FakeDevice();
        hardware.usb = true; hardware.limit = 1;
        tile = new ChargingSeparationTileService(); tile.onStartListening();
        FixtureState.failStart = true; tile.onClick();
        check(!enabled() && hardware.limit == 1 && tile.tile.subtitle.equals("OFF failed · open app"),
                "rejected OFF must warn while native separation remains active");
        // A fresh process has no trustworthy static state; reopening the dashboard
        // starts the existing controller's OFF reconciliation, including readback.
        BypassService.current = BypassController.State.OFF; BypassService.published = false;
        Handler.next.run();
        check(tile.tile.subtitle.equals("OFF failed · open app"), "restart must retain OFF failure warning");
        BypassControllerTest.FakeSettings saved = new BypassControllerTest.FakeSettings();
        saved.desired = enabled(); saved.requested = true;
        BypassControllerTest.FakePlatform platform = new BypassControllerTest.FakePlatform();
        BypassController recovery = new BypassController(hardware, saved, platform);
        recovery.start("dashboard OFF reconciliation after restart");
        check(hardware.limit == 0 && !saved.requested && recovery.state() == BypassController.State.OFF
                && platform.published.contains(BypassController.State.OFF), "OFF recovery must confirm native zero");
        BypassService.current = recovery.state(); BypassService.published = true;
        FixtureState.prefs.edit().remove(SeparationControl.PREF_START_ERROR).apply();
        check(tile.tile.subtitle.equals("Off · restored"), "confirmed controller restore must refresh tile");
        tile.onStopListening();
        android.os.Build.VERSION.SDK_INT = 28;
        FixtureState.prefs.values.put(BypassService.PREF_DESIRED, true);
        tile.onStartListening();
        check(tile.tile.state == Tile.STATE_ACTIVE, "Android 9 must support toggle without subtitle API");
        tile.onStopListening();
        System.out.println("Quick Settings lifecycle, unlock, shared control and failure regressions passed.");
    }
}
