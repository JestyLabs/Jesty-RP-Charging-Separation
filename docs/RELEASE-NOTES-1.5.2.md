# Jesty RP Charging Separation 1.5.2

This update fixes mode selection after toggling bypass and polishes the
landscape layout.

## Changes

- Turning **BYPASS CHARGING** off now leaves the selected **RIGHT AWAY** or
  **AT A BATTERY LEVEL** mode saved. Both choices remain grayed out while bypass
  is off, and the previous selection is restored when it is turned on again.
  Reboot persistence still turns off with bypass.
- The helper text under **RIGHT AWAY** now says “Stops active charging to the
  battery.”
- The scroll indicator is hidden while swipe scrolling remains available.
- The live dashboard is wider on landscape screens to reduce the gap beside the
  controls.

## Verification

- Java charge-limit policy tests and the Android build passed.
- ZIP alignment, package `com.jesty.rpchargingseparation`, version `1.5.2`
  (`versionCode 21`), APK Signature Scheme v3, and the existing signing
  certificate were verified.
- No Retroid was connected by ADB during this build. The exact 1.5.2 toggle
  sequence and visual layout have not yet been checked on device.

## APK

- File: `Jesty-RP-Charging-Separation-1.5.2.apk`
- Size: `6,058,927` bytes
- SHA-256: `A9DAD181872481E862F8BB5D324C2C9E34EE0A9099CBEB5481B22E8B4F0D5A09`
