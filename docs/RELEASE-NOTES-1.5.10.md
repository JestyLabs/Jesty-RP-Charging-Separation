# Jesty RP Charging Separation 1.5.10

This release restores and verifies normal charging (`charge_control_limit=0`)
before the app shows **READY** after USB is unplugged during active separation.
The bypass preference remains on, so the selected mode can resume on the next
plug-in. If the native restore cannot be confirmed, the app shows a failure
instead of reporting READY.

**COPY DIAGNOSTICS** now includes recent Android process exit reasons and a
read-only view of Retroid process-protection settings. This helps investigate
background-service termination without changing those settings.

On the reported Flip 2 12 GB firmware 1.0.0.311, add the app to **Settings →
Handheld Settings → Advanced → Whitelist application** to keep the charging
monitor active while the screen is off. See [issue #2](https://github.com/JestyLabs/Jesty-RP-Charging-Separation/issues/2).
The issue remains open for confirmation of the underlying service behavior.

The charging-controller scenarios, GitHub CI tests, and unsigned Android build
passed. The exact signed APK was installed over v1.5.9 on a Flip 2 `.130`:
Right away mode restored limit `0/10` and showed READY on unplug, then returned
to `10/10` and **Not charging** on reconnection. Turning bypass OFF restored
normal charging. Automatic mode and Force Stop were not repeated on this APK.

The APK must be signed with the existing release certificate so it can install
over earlier versions without clearing app data.

APK SHA-256: `71D7DF7F46356A5CE5F5A3C27AB9D112C2CB369E56BC00A79639F87F790BCED0`
