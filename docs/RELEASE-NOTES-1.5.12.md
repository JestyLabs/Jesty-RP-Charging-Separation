# Jesty RP Charging Separation 1.5.12 — updater reliability

Maintenance candidate: the top-right **CHECK UPDATES** action remains available even
when no update has been found. A successful newer-release check changes it to
**UPDATE vX**; users see progress, retry state and a manual up-to-date message.
Re-entering the foreground checks again when due, without background polling.

The app checks GitHub's latest stable release with HTTPS and bounded response
data, retaining existing trusted URL, APK digest, archive signature and Android
installation-confirmation checks. Successful checks are cached for one hour.
Failed checks are logged and can retry automatically after five minutes or
immediately on a manual request. The automatic popup is postponed until the app
is in the foreground and is marked prompted only when displayed.

The temperature card now shows the observed battery-temperature minimum and
maximum, kept in app-private preferences across restarts. Samples use the
existing activity and active-service telemetry reads; there is no additional
timer, background worker or polling. The **RESET TEMP MIN/MAX** option starts
a new recording period with confirmation. Only new extremes are persisted;
invalid sensor readings are excluded. Diagnostics include the observed range.
These are observed readings (not lifetime battery records), and are not used
to decide when charging starts or stops.

No charging controller, native bridge, boot receiver, or Quick Settings
charging control is modified. The foreground service's existing telemetry read
now records the sample on a best-effort basis, without changing its charging
decisions.

New JDK tests use a local simulated GitHub endpoint to cover success, HTTP
failure, oversized body and recovery, alongside retry-policy checks. The
existing JDK suites and unsigned Android APK build run in GitHub CI.

## Release gates (not automatically fulfilled)
- Sign with the existing Jesty RP release certificate, not a fresh key.
- Verify versionCode **33**, versionName **1.5.12**, package name, APK digest and signer.
- On real Retroid hardware, upgrade over v1.5.10 or v1.5.11 without clearing
  charging preferences, and exercise manual/offline/online checks, popup,
  cancellation, Android installation and normal charging behavior.
- Independently reproduce v1.5.10 → v1.5.11 update detection if investigating
  the original reporter's behavior. This diagnostic is not a substitute for
  testing the exact v1.5.12 candidate.
- Only then publish the matching signed artifact and checksum on GitHub.
