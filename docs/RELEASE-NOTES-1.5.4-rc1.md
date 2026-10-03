# Jesty RP Charging Separation 1.5.4-rc1

Testing pre-release for process recovery and dock power negotiation. The
signed APK updates an existing installation in place (`versionCode 23`).

- A sticky foreground-service restart reads the saved switch state and adopts
  an already active Retroid native bypass, including the automatic limit's
  charge-again hysteresis.
- Opening the app reads the native control. A saved OFF choice restores a
  leftover native limit; an explicit OFF still restores normal charging.
- A dock's initial `Discharging` status gets a short settling period before
  native separation is applied or declared failed. A persistent mismatch still
  restores normal charging.
- Sustained battery discharge still shuts bypass OFF for safety and now leaves
  a visible reason. Lifecycle logs distinguish this from a process death.

**Validated on a Flip 2:** in-place update, adoption of an active native bypass,
Android restart after a controlled `am crash`, notification recreation, explicit
OFF restoring limit `0`, and automatic 80%/65% hysteresis. The observed vendor
Recents action used Force Stop; Android did not restart the service until the
app was opened. A true low-memory kill and the RP5 dock transition remain to be
tested. See [process recovery](PROCESS-RECOVERY.md) for evidence and commands.
