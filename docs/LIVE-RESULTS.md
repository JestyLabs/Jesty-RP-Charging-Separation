# Live validation status

The exact v1.5.1 APK was built and signed on Windows without a connected
Retroid. Its compact layout and dependent-control behavior still need a
physical screen and interaction check.

## Community compatibility reports

The maintainer reports testing with help from owners in the
[Retroid community thread](https://www.reddit.com/r/retroid/comments/1wrl00f/noroot_charging_separation_app_for_rp5_flip_2/).
The app is reported compatible with Retroid Pocket 5, Flip 2, Pocket Mini, and
Pocket Mini V2. The Flip 2 has the instrumented charging-separation capture
below; the other model reports do not yet have publishable, sanitized telemetry
here. The evidence recorded in this repository does not document a complete
automatic charge/resume cycle on the exact v1.5.0 APK for each model.

## Confirmed foundations

- Retroid's `PServerBinder` is reachable through the hidden Android service
  manager on the original development hardware.
- The vendor transaction expects a two-element String array and transaction code
  `0`.
- Capability checks replace the old model-name whitelist.
- The RP5 inspection observed readable native limit nodes with a `0/10` state.
- The maintainer subsequently reported a successful functional test on a
  friend's RP5. This is recorded as user-reported compatibility until a
  sanitized telemetry capture from that device is available.

## Confirmed on Flip 2 with the public build

- The signed `1.3.0-dev` public package installed successfully alongside the
  earlier private package.
- Enabling changed the native limit from `0/10` to `10/10` and Android status
  from `Charging` to `Not charging`.
- Disabling restored `0/10` and `Charging`.
- Thirty-sample captures recorded +0.2388 A mean battery current during normal
  charging and -0.0111 A during separation at a stable 29.7 C.
- Only the public package had an active control service during the capture.

## Further device evidence to collect

- Publishable RP5, Pocket Mini, and Pocket Mini V2 telemetry captures are still
  pending; community compatibility reports are not independent benchmarks.
- Reboot, unplug/replug, service-destruction, and restore behavior must be
  repeated after the final build is signed.

No pending item should be described as passed until a real-device run records
the final state.

## 1.4.0 artwork and dashboard candidate

- Installed in place on the physical Retroid Pocket Flip 2 as
  `versionCode=15`, `versionName=1.4.0`.
- Normal charging showed positive battery flow and `Charging`.
- Enabling separation changed Android to `Not charging`, with battery flow
  approximately neutral while USB continued powering the device.
- Disabling restored normal charging.
- Swiping the app itself away from Recents kept the controller active.
- The vendor **Clear all** action behaved like an Android force-stop on this
  firmware and stopped the service; reopening the app allows it to start again.
- The final signed rebuild only changes alignment, corner copy, and disclosure
  text. Package identity was rechecked after installation; the full hardware
  sequence was not repeated for those presentation-only changes.
