# Jesty RP Charging Separation 1.3.0 pre-release

This update refreshes the dashboard and public project branding while keeping
the charging-control and fail-safe implementation unchanged.

## What changed

- Approved horizontal Jesty RP Charging Separation lockup.
- New maintainer-selected separated/normal charging backgrounds.
- Yellow brand styling for the dashboard toggles.
- Compact project/support footer.
- Same public package and signing certificate for an in-place update.

## Important validation status

This is a **pre-release**. The exact downloadable 1.3.0 APK is published before
its post-release Pocket Flip 2 checklist is complete. Privileged control,
readback validation, ongoing telemetry, and fail-closed restoration remain
unchanged. The RP5 has been reported working, but this release does not claim a
new maintainer telemetry capture for that model.

## Everyday use

- No Magisk, terminal, or user-managed root setup.
- The dashboard does not need to remain open.
- Swiping the app away from Recents does not disable the controller.
- Reboot restore occurs only when **Restore after a normal reboot** is enabled.
- Android Settings -> Force stop is different and blocks automatic operation
  until the app is opened again.

Package: `com.jesty.rpchargingseparation`  
Version: `1.3.0` (`versionCode 14`)

