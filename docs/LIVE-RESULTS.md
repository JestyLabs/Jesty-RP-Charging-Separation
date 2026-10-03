# Live validation status

On Flip 2 firmware 1.0.0.130, the exact signed v1.5.8 APK held its partial CPU
wake lock while the screen was asleep and automatic mode was charging toward
85% (`limit=0`, Android `Charging`). Lowering the stop below the current level
engaged native bypass (`limit=10/10`, `Not charging`) and released the lock.
After a controlled process crash during charge-to-limit, Android restarted the
sticky service and it reacquired the lock. The original 80%/65% settings were
restored with bypass active. This did not reproduce a full asleep charge cycle
or the issue reporter's stated firmware 1.0.0.311, whose full build ID has not
been independently verified.

The exact signed v1.5.7 APK was installed in place on a Flip 2. Its LIVE card
showed the status beside the heading and kept the device and detail lines
aligned at full width. GITHUB appeared before SUPPORT. With USB connected,
the native limit was `10/10`, battery status was `Not charging`, and the
foreground service remained active. The narrow-screen layout was not tested.

The published v1.5.6 APK was installed on a Flip 2 through the new in-app
updater, starting from an updater-enabled v1.5.5 test build (the public v1.5.5
did not include the updater). GitHub release discovery, install-source
permission, APK download and verification, Android confirmation, and the
in-place update completed. The installed package reported v1.5.6/code 27.
With USB connected, the native limit remained `10/10`, battery status was
`Not charging`, and the foreground service and notification were active. The
app showed `RUNNING FROM USB`. This test covers the Flip 2, not other models.

The v1.5.5 shared dashboard/status card was visually checked on a Flip 2 with
the exact signed stable APK installed in place. The screenshots below show the
preceding v1.5.2 layout. Process recovery and explicit OFF were also checked
on the Flip 2;
see [process recovery](PROCESS-RECOVERY.md) for the observations and limits.

The exact v1.5.2 APK was built and signed on Windows without a connected
Retroid during the release round. The maintainer subsequently supplied two
Flip 2 screenshots of v1.5.2: [Right away](../assets/screenshots/flip2-v1.5.2-right-away.png)
and [charging toward 80%](../assets/screenshots/flip2-v1.5.2-charging-to-80.png).
They show the compact layout, selected modes, and a `Charging` state at 17%
with an 80% stop and user-set 65% resume level. They do not show the actual
stop/resume transitions or prove the dependent controls cannot be changed while
bypass is off.

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
