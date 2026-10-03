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
