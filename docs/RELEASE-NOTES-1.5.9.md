# Jesty RP Charging Separation 1.5.9

This release improves automatic charging separation when bypass is enabled
before the charger is connected. The foreground service now reacts to battery
and power events while the screen is asleep. If a charger or telemetry check
fails temporarily, it restores normal charging, keeps the reason visible, and
tries again instead of silently stopping.

The app also adds **COPY DIAGNOSTICS** and a warning when Android restricts it
in the background. The dashboard status is larger, says **RUNNING FROM
CHARGER** when separation is active, and no longer has the small LIVE label.
The app screen can time out normally.

## Flip 2 check

On a Retroid Pocket Flip 2 with firmware
`RPFlip2_V1.0.0.130_20250501_121708_user`, we enabled bypass while unplugged,
turned the screen off, then connected the charger at 75%. The service detected
power and kept monitoring. At 80%, it enabled and verified native separation
without reopening the app. The battery then showed **Not charging** with the
native limit at `10/10`. The final signed 1.5.9 APK was installed in place and
its dashboard was checked at 80%.

![Flip 2 at 80%, running from charger](https://github.com/JestyLabs/Jesty-RP-Charging-Separation/releases/download/v1.5.9/flip2-v1.5.9-80percent.png)

The reporter's firmware has not yet been tested. [Issue #2](https://github.com/JestyLabs/Jesty-RP-Charging-Separation/issues/2)
stays open for their confirmation. More test detail is in
[DEVICE-VALIDATION.md](https://github.com/JestyLabs/Jesty-RP-Charging-Separation/blob/v1.5.9/docs/DEVICE-VALIDATION.md).

The APK is signed with the same certificate as previous releases. Install it
over the existing app to keep settings.

APK SHA-256: `C3EF059D038E9EF45715DE512A6C871ADD0EF457BD81F38B418C33FF88654A25`
