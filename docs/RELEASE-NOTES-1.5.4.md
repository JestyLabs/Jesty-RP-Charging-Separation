# Jesty RP Charging Separation 1.5.4

This stable release promotes the process-recovery changes tested in
`1.5.4-rc1`. The app uses `versionCode 24` and updates existing installations
in place.

- After a normal process death, Android's sticky foreground-service restart
  reads the saved switch state, reattaches to Retroid's native bypass, restores
  monitoring and the notification, and preserves automatic limit hysteresis.
- Reopening the app reconciles the saved switch choice with the native charging
  control. Explicit OFF still restores normal charging.
- USB/dock status has a short settling period before native bypass validation.
  A persistent mismatch still fails safely and restores normal charging.
- A sustained battery discharge still disables bypass; the dashboard and logs
  now explain that safety stop.

## Validation

- On a Retroid Pocket Flip 2, an in-place update adopted an already active
  `10/10` native limit. A controlled `am crash` caused Android to recreate the
  service, reattach to bypass and restore the notification. The native limit
  stayed `10/10` with status `Not charging`.
- Explicit OFF restored limit `0` and status `Charging`. Automatic mode charged
  toward the 80% stop level. A temporary 75% stop engaged bypass at 77%; after
  restoring 80%, bypass remained active until the configured 65% resume level.
- The final stable APK was built and signed with the existing certificate.
  Its source differs from the tested RC only in version metadata and release
  documentation. The Flip 2 disconnected from ADB before this exact APK could
  be installed for a final smoke check.

The Flip 2 launcher was observed issuing Android Force Stop from Recents. The
app intentionally does not restart itself after Force Stop; open it again to
resume monitoring. A true low-memory kill and the reported RP5 dock transition
have not been reproduced with this release. See
[process recovery](PROCESS-RECOVERY.md) for details.
