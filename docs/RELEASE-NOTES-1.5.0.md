# Jesty RP Charging Separation 1.5.0

This update keeps the optional automatic charging limit from 1.5.0-rc1 and
redesigns the dashboard around simpler controls and live readings.

## What changed

- **Right away** and **At a battery level** replace the technical mode names.
  The latter stops charging at a chosen level (80% by default) and resumes at
  another (70% by default), always at least five points lower.
- Numeric fields and 5-point step buttons replace the sliders. Out-of-range
  values are adjusted with an on-screen explanation.
- A wide landscape display keeps the LIVE readings in a right-hand panel;
  narrower displays stack them beneath the controls.
- The logo and app version are centered. Status, notification, and dashboard
  labels use shorter wording.
- The saved 1.5.0-rc1 resume margin is migrated to an equivalent charge-again
  percentage. The package and signing identity remain the same, while
  `versionCode 19` permits an in-place update from rc1 (`versionCode 18`).

## Validation status

- The Java policy tests, Android build, ZIP alignment, manifest inspection,
  and APK signature verification passed on Windows.
- No Retroid was connected for this release round. The exact APK has not been
  installed, its automatic charge/resume cycle has not been verified on device,
  and the redesigned screen has not been visually checked on a Retroid.
- Before promoting the new behavior as device-validated, test an update from
  1.4.2 and rc1, the two threshold transitions, manual mode, USB reconnect,
  background operation, reboot restoration, and the final screen layout.

## Artifact

- Package: `com.jesty.rpchargingseparation`
- Version: `1.5.0` (`versionCode 19`)
- APK: `Jesty-RP-Charging-Separation-1.5.0.apk`
- APK SHA-256:
  `11A12BC9F6EE3F10C8B72B7FE3E7DC1BD4FBFABF304E86177E19EC2E543EA299`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.
