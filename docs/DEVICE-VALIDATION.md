# Device validation checklist

Record the exact model, firmware build, charger, cable, battery percentage, and
ambient conditions. Redact serial numbers and account information.

The maintainer reports compatible operation on Pocket 5, Flip 2, Pocket Mini,
and Pocket Mini V2 with help from owners in the
[Retroid community thread](https://www.reddit.com/r/retroid/comments/1wrl00f/noroot_charging_separation_app_for_rp5_flip_2/).
The checklist below is for collecting reproducible evidence for the exact
v1.5.0 APK and its new automatic threshold mode; community reports do not
mark these individual checks complete.

For v1.5.2, also check the compact controls on the actual screen: with Bypass
charging off, the mode and reboot options are gray and cannot be changed;
turning it off clears reboot persistence but remembers the selected mode;
turning it back on restores the same mode; and the LIVE DASHBOARD is visible
near the controls at the bottom right on wide landscape screens.

The v1.5.3 charging status placement immediately above the LIVE DASHBOARD was
visually checked on a wide Flip 2 screen using v1.5.4-rc1. The narrow-screen
placement still needs checking. The v1.5.2 screenshots predate this change.

For v1.5.5, the status is inside the same LIVE DASHBOARD card, beside its title
and device line on a wide Flip 2 screen. The narrow-screen layout still needs
checking.

For v1.5.7, the simplified LIVE card and GITHUB/SUPPORT button order were
visually checked on a wide Flip 2 screen. The narrow-screen layout still needs
checking.

For v1.5.8, test automatic mode across the stop level with the screen asleep,
especially on the stated Flip 2 firmware 1.0.0.311 in issue #2. Confirm native
limit changes from `0` to `limitMax` near the selected percentage. The local
Flip 2 uses 1.0.0.130 firmware; its screen-off wake lock and simulated
threshold transition were checked, but a full charge cycle on the reporter's
device remains open. The reported .311 full build ID is unverified.

For the issue #2 reliability changes (v1.5.9), check on Flip 2 firmware
1.0.0.311 if possible, with the screen off and without opening the app during
each run. Attach **COPY DIAGNOSTICS** output to each result.

- [ ] On the reporter's .311 firmware, bypass turned on while unplugged (READY),
  screen off, then plug in:
  charging stops at the stop level without reopening the app.
- [ ] Bypass turned on while plugged in below the stop level, screen off: the
  native limit changes from `0` to `limitMax` within one percentage point.
- [ ] After stopping, the battery drops to the charge-again level with the screen
  off and charging resumes, then stops again.
- [ ] With the app restricted in App info → Battery, the warning appears.
- [ ] After a reboot with boot restoration ON and USB unplugged, the service is
  READY and plugging in works with the screen off.
- [ ] A forced temporary failure shows TRYING AGAIN and recovers by itself.

On a Flip 2 with build `RPFlip2_V1.0.0.130_20250501_121708_user`, a signed
1.5.9-rc1 test build was installed over 1.5.8 without clearing data. Bypass
was turned off (`limit=0`), USB was unplugged, and bypass was enabled while
unplugged (READY). On plugging in at 75%, the foreground service logged
`USB detected (ACTION_POWER_CONNECTED)` and held the charge-to-limit CPU wake
lock. The screen was put to sleep; the device then charged on a wall charger
without reopening the app. At 80%, the log recorded `Stop level reached`,
`Enabled and verified native separation`, and wake-lock release. On reconnect
to ADB, battery status was `Not charging` and the native limit was `10/10`.
The final signed 1.5.9 build was then installed over the test build without
clearing data. Its dashboard showed RUNNING FROM CHARGER at 80%, the battery
was Not charging, and the service was still monitoring. Its charging code is
the same as the tested build; only the UI and screen timeout changed.
The [80% screenshot](../assets/screenshots/flip2-v1.5.9-80percent.png) shows
that final build.

The reporter retested v1.5.9 on
`RPFlip2_HV1.0.0.311_20260725_132332_user`. At 11:07, the service logged
`USB detected (BATTERY_CHANGED) at 65%` and `Holding CPU awake until the stop
level`. The next saved event was a fresh `Start (ENABLE)` at 12:04, when the
app was reopened at 99%; separation activated then. The notification had
disappeared while the app was closed. No `Service destroyed` or sticky restart
appears in the saved events between those times. This strongly suggests that
the process or service stopped, but the diagnostics do not establish whether
the cause was memory pressure, a crash, a Recents/Task Manager action, or a
vendor policy. `Background restricted: false` does not rule out those causes.

The same reporter then isolated Retroid's two vendor process-protection
controls. With only **Clean process when standby -> Ignored packages** enabled,
the service survived and stopped charging at 80%. With only **Whitelist
Application** enabled, it also survived and stopped at 80%; Whitelist
Application additionally preserved the foreground notification when the
dashboard was closed. This proves that either vendor protection was sufficient
for the tested Flip 2 12 GB .311 session. It does **not** prove which setting
key or storage format backs either Retroid UI.

### Read-only Retroid Whitelist Application backend proof

The diagnostic branch intentionally does not change process-protection state.
**COPY DIAGNOSTICS** performs one privileged read, `settings list system`,
through the already-used `PServerBinder`, then filters the result locally.
`app_whiteList` is reported as an OdinTools-derived candidate only; do not call
it the Retroid backend until a physical before/after test proves the link.

On each firmware under test, capture these three samples without changing
charging settings:

1. Remove Jesty RP Charging Separation from **Whitelist Application** and from
   **Clean process when standby -> Ignored packages**. Copy diagnostics.
2. Add only Jesty RP Charging Separation to **Whitelist Application** using the
   Retroid UI. Copy diagnostics again.
3. Remove it from **Whitelist Application** again. Copy diagnostics a third
   time.

Compare the **Relevant system settings** blocks. The Whitelist Application
backend is proven only when an exact package token or other setting change
appears when the UI entry is added and reverses when it is removed. Repeat the
same one-variable sequence for **Clean process when standby -> Ignored
packages** to identify that separate backend. Prefer evidence from both the
maintainer Flip 2 .130 and reporter Flip 2 .311 before adding any write path.
A read failure or missing `app_whiteList` remains **unknown**, not
"unsupported" or "unprotected".

For an APK-independent proof, the branch also includes a read-only ADB capture
harness. It never writes Settings or charging controls:

```powershell
.\scripts\capture-retroid-process-protection.ps1 -Label whitelist-off-1
# Add only Jesty RP Charging Separation in Retroid -> Whitelist Application.
.\scripts\capture-retroid-process-protection.ps1 -Label whitelist-on
# Remove it again.
.\scripts\capture-retroid-process-protection.ps1 -Label whitelist-off-2
```

It records only model/build metadata plus `settings list system/global/secure`,
the direct `app_whiteList` candidate read and the current device-idle whitelist.
The capture folder is append-only by label so a later run cannot silently replace
earlier evidence.

Compare adjacent captures:

```powershell
.\scripts\compare-retroid-process-protection.ps1 `
  -Before .\build\retroid-process-protection\whitelist-off-1 `
  -After  .\build\retroid-process-protection\whitelist-on

.\scripts\compare-retroid-process-protection.ps1 `
  -Before .\build\retroid-process-protection\whitelist-on `
  -After  .\build\retroid-process-protection\whitelist-off-2
```

A credible backend mapping must change in the expected direction on the first
transition and reverse on the second. A one-way difference is insufficient
because unrelated firmware state can change between samples.

Repeat with new labels for **Clean process when standby -> Ignored packages**,
again changing only that one Retroid control.

- [ ] Normal charging is confirmed before enabling.
- [ ] Enable reaches `Not charging` and `limit == limitMax`.
- [ ] Battery current settles within the confirmation threshold.
- [ ] USB input and estimated device power remain plausible under load.
- [ ] A sustained unsafe battery discharge disables separation.
- [ ] Manual disable restores `charge_control_limit` to `0`.
- [ ] Closing the dashboard and removing it from recents preserves the active
  foreground service.
- [ ] Unplug and replug transitions between ARMED and ACTIVE safely.
- [ ] Reboot with boot restoration OFF remains normal.
- [ ] Reboot with boot restoration ON reconciles correctly after USB is present.
- [ ] Force stop behavior and recovery after opening the app are documented.
- [ ] The device charges normally after all tests.

### v1.5.0 automatic threshold mode

- [ ] Normal charging continues below **Stop charging at** (80% by default).
- [ ] At that level, charging separation activates and USB continues powering
  the handheld.
- [ ] At **Charge again at** (70% by default), normal charging resumes.
- [ ] A second cycle reaches the stop level again without manual intervention.
- [ ] Manual **Right away** mode, USB reconnect, background service, and reboot
  restoration still behave as expected.
- [ ] The redesigned layout is readable on the model's screen.

## Pocket Mini V2 1.4.1 compatibility check

Status: pending an owner test of the exact signed 1.4.1 APK.

- [ ] Diagnostic line identifies the Pocket Mini V2 model.
- [ ] App reads `charge_control_limit` and `charge_control_limit_max`.
- [ ] Enabling reaches `CHARGING SEPARATED`, `Not charging`, and matching
  current/maximum limit values while USB input remains above zero.
- [ ] Battery flow remains near neutral rather than sustaining discharge.
- [ ] Unplug/replug returns to the armed state and re-enables safely.
- [ ] Disabling restores limit `0` and normal `Charging` behavior.
- [ ] A normal reboot with restore disabled leaves normal charging active.

Do not mark the Mini V2 as validated from chipset similarity or a successful
install alone; the charging state, power flow, and restoration checks must pass.

## 1.4.2 launcher-icon release

The signed 1.4.2 artifact was built from the reviewed public tree and passed
package, version, alignment, resource, and signature verification. This release
changes only the launcher vector and version metadata. The notification icon,
charging-control code, safety state machine, monitoring, and boot behavior are
byte-for-byte unchanged from 1.4.1.

The exact 1.4.2 APK was not installed on the physical Flip 2 during this release
round. Existing Flip 2 charging validation therefore remains functional evidence
for the unchanged control path, not a claim that the new launcher icon was
visually checked on-device.
