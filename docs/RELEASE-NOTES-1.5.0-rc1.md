# Jesty RP Charging Separation 1.5.0-rc1 (pre-release)

This testing build adds an optional **Automatic limit** mode. The existing
**Immediate** mode remains the default for installations updating from 1.4.2.

- Set the separation point from 30% to 100% (80% by default).
- Set the resume margin from 1 to 20 percentage points (10 points by default).
- In automatic mode, normal charging is allowed below the limit. At the limit,
  the app enables and validates native charging separation. At `limit - margin`,
  it restores normal charging and waits for the next cycle.
- The existing boot-restoration preference still controls whether the service
  starts after a normal reboot.
- Removed the dashboard badge line asking users to support testing or star the
  project.

## Validation status

- Pure policy tests, the Android build, ZIP alignment, and signed APK
  verification passed on Windows.
- Package: `com.jesty.rpchargingseparation`; version `1.5.0-rc1` (code 18).
- The automatic threshold cycle has **not** been validated on a physical Flip 2
  or RP5. Verify charging, separation, resume, USB reconnect, app closure, and
  reboot behavior before promoting this feature to a stable release.

## Artifact

- APK: `Jesty-RP-Charging-Separation-1.5.0-rc1.apk`
- Signed APK SHA-256:
  `BF2AC549B28C9AD850D0C6F6F4459EE2CA69E4EFFE14B547DD76E412FE1C7E24`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.
