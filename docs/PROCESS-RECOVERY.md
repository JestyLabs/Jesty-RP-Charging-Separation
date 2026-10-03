# Process recovery check on a Retroid

## Flip 2 observation, 2026-10-03

The connected Retroid Pocket Flip 2 had v1.5.2 installed, USB present, native
limit `10/10`, status `Not charging`, and no foreground service or active
notification. Its Android log showed `Force stopping
com.jesty.rpchargingseparation ... from pid` of the launcher Recents process.
This is evidence of the vendor's Recents/Clear all path invoking Force Stop;
it is not evidence of a low-memory kill. The package's stopped flag was true.

After an in-place update to signed `1.5.4-rc1` and a manual open, the service
logged `Reattached to existing native separation`; the limit stayed `10/10`
and the foreground notification returned. `am crash` then caused an abrupt
process death while bypass was active. Android restarted the sticky service
about one second later with a null intent; the new process again reattached to
native separation, and the notification returned. The limit remained `10/10`
and status `Not charging` through that test. Explicit OFF restored limit `0`
and status `Charging`. Explicit ON in automatic mode at 77% correctly charged
toward 80%; lowering the stop to 75% engaged bypass, and restoring 80% kept it
active until the configured 65% charge-again level.

This validates abrupt process-death recovery and explicit OFF on this Flip 2.
It does not reproduce a true `lmkd` event. A separate RP5 dock report shows
v1.5.2 briefly returning `Discharging` during initial validation; the rc1
settling wait still needs a dock cycle on that RP5. Android Force Stop prevents
automatic service restart; the native Retroid limit can remain active until
the user opens the app, so the app cannot monitor battery current during that
interval.

## Reproduction steps

Use a supported RP5, Flip 2, Mini, or Mini V2 with USB power connected. Record
the app version, selected mode and thresholds, battery percentage, and whether
the notification is present. Do not run these checks on an unrelated Android
device. Keep the charger attached during each test.

```powershell
adb devices -l
adb logcat -c
adb shell pidof com.jesty.rpchargingseparation
adb shell cat /sys/class/power_supply/battery/charge_control_limit
adb shell cat /sys/class/power_supply/battery/charge_control_limit_max
adb shell cat /sys/class/power_supply/battery/status
```

Run the RAM-heavy emulator until the notification disappears, then immediately
capture a bug report or logcat before reopening Jesty:

```powershell
adb logcat -d -v threadtime > charging-process.log
adb shell dumpsys activity services com.jesty.rpchargingseparation
adb shell pidof com.jesty.rpchargingseparation
adb shell cat /sys/class/power_supply/battery/charge_control_limit
adb shell cat /sys/class/power_supply/battery/status
```

Look for `lmkd`/`lowmemorykiller` and `ActivityManager` process-death lines,
`JestyRPCharging` service start/destroy lines, and especially `Safety stop:
battery powering device`. The safety line means sustained battery discharge
turned bypass OFF deliberately; process death alone should leave the saved
switch ON. A new service start should restore the notification and adopt an
already active native bypass. At a battery level, the service should preserve
the stop/charge-again hysteresis across that restart.

Test Recents swipe and the vendor launcher's **Clear all** separately while the
service is active. Record whether `onTaskRemoved` appears and whether the
service/notification survives. Those launcher actions can vary by firmware.
Finally, use Android Settings -> Apps -> Jesty -> **Force stop** as a separate
control: automatic restart is not expected until the user opens Jesty again.
After reopening, confirm that the switch and displayed state match the saved
intent and native control. Explicitly turn bypass OFF and confirm the native
limit reads `0`.

Repeat these checks on each relevant firmware, particularly a true memory
pressure event and an RP5 dock transition. A desktop build alone cannot
establish native-state persistence or launcher behavior.
