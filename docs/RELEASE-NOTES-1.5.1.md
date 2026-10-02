# Jesty RP Charging Separation 1.5.1

This update simplifies the controls and live dashboard for landscape displays.

## What changed

- Bypass charging, threshold mode, and reboot persistence now share one left
  panel. **Maintain bypass charging after reboot** is the last option.
- Turning Bypass charging off grays out the other controls, turns reboot
  persistence off, and resets the mode to **Right away**. The two battery
  percentages stay saved for when threshold mode is selected again.
- On wide screens, **LIVE DASHBOARD** sits at the bottom right. The device name
  and charging state appear under the heading. The footer badge and diagnostic
  legend have been removed.

## Verification

- Java charge-limit policy tests and the Android build passed.
- ZIP alignment, package `com.jesty.rpchargingseparation`, version `1.5.1`
  (`versionCode 20`), APK Signature Scheme v3, and the existing signing
  certificate were verified.
- No Retroid was connected by ADB during this build. The exact 1.5.1 layout,
  control interactions, and charging cycle have not been checked on device.

## APK

- File: `Jesty-RP-Charging-Separation-1.5.1.apk`
- Size: `6,058,927` bytes
- SHA-256: `ED467719B7810EB399D6952A8E8F128C696395965D8CB2228233D2FE614A1021`
