# Architecture

## UI process

`MainActivity` owns only the dashboard, user switches, and one-second telemetry
refresh while visible. The left controls share one panel. The live dashboard
is anchored at the bottom right on wide landscape screens, with charging status
beside its title and device line inside the same card. On narrower screens, status stays above
the controls and the dashboard stacks beneath them. When bypass is off,
dependent controls are disabled, reboot persistence is cleared, and the
selected mode and two saved
battery percentages are retained for the next enable. The scroll indicator is
hidden while swipe scrolling remains available.
Closing the Activity does not represent the controller's requested state.

## Foreground controller

`BypassController` is the state machine. It has no Android dependencies, so
`scripts/test-bypass-controller.ps1` runs it against a fake Retroid power supply
and clock. `BypassService` is a thin foreground-service adapter around it that
provides the sysfs/root access, preferences, wake locks, notification, and
timing.

```text
OFF -> ENABLING -> ACTIVE
               -> CHARGING_TO_LIMIT (automatic limit, below limit)
               -> ARMED (no USB)
               -> RETRYING (temporary failure; normal charging, retry with backoff)
               -> FAILED (unsupported device or restore failure; service stops)

ACTIVE <-> CHARGING_TO_LIMIT (automatic limit hysteresis)
```

In **Right away** mode the service separates as soon as USB is present. In
**At a battery level** mode it keeps normal charging until the configured stop
level (default 80%), separates, and restores normal charging at the charge-again
level (default 70%, always at least five points below the stop level).

It validates the native controls, performs privileged writes, confirms
readback/status, and re-evaluates telemetry on every battery level change and
USB plug/unplug event, with a two-second poll as a fallback. When a dock first
reports USB present but the battery still says Discharging, the service waits briefly for power negotiation before
writing the native limit. It also allows a short settling period for the
post-write `Not charging` status; a persistent mismatch still restores normal
charging. If bypass is still wanted, the controller keeps normal charging and
retries no sooner than 15 s, doubling to at most 5 minutes; reaching ACTIVE
resets the delay. While asleep, a retry may wait for the next power event or
wake. Only an unsupported device or a failed restore ends the service; its
notification is detached so the reason stays visible.

The two-second executor uses a clock that pauses in deep sleep, so it cannot
be relied on with the screen off (issue #2). The service therefore registers
`ACTION_BATTERY_CHANGED`, `ACTION_POWER_CONNECTED`, and
`ACTION_POWER_DISCONNECTED` at runtime. Each event holds a short partial wake
lock until the resulting check finishes. Manifest receivers for these
broadcasts are not delivered to apps targeting API 26+, so `BootReceiver`
starts the service when bypass is on, even without USB.
Whether the firmware delivers `ACTION_BATTERY_CHANGED` for each percentage step
during deep sleep still needs checking on each device. For that reason, while
`CHARGING_TO_LIMIT` is active the service also holds a partial CPU wake lock so
the stop level is still checked during screen-off charging.
It releases the lock after bypass activates, USB disconnects, bypass is
disabled, or the service ends. A sticky service restart in this phase acquires
the lock again. This trades some plugged-in power use for a timely threshold.

## In-app updates

`AppUpdater` reads `GET /repos/JestyLabs/Jesty-RP-Charging-Separation/releases/latest`
when the Activity opens, at most once an hour, and caches the last result so
the top-bar **UPDATE** button can reappear without a new request. Drafts,
pre-releases, unparseable tags, and assets without a GitHub `sha256:` digest or
outside this repository's `releases/download/<tag>/` path are ignored.
`UpdateVersion` holds the Android-free version comparison and URL/digest checks
covered by `scripts/test-update-version.ps1`.

An update is only downloaded after the user taps **Update**. The APK is written
to the app cache, checked against the published size and SHA-256, and parsed to
confirm the package name, a higher `versionCode`, and a signing certificate the
installed app already has. It is then committed through a `PackageInstaller`
session; `UpdateInstallReceiver` shows Android's confirmation screen and
reports failures. Android remains the final authority on the signature. The
`REQUEST_INSTALL_PACKAGES` permission requires the user to allow this app as an
install source once. Replacing the package kills the process;
`MY_PACKAGE_REPLACED` lets `BootReceiver` resume a desired bypass afterwards.

## Diagnostics

`EventLog` keeps the last 400 state transitions and events in the app's private
`events.txt`, with no serials or account data. **COPY DIAGNOSTICS** copies them
with the app version, model, firmware build, Android version, settings,
background-restriction state, and current readings, so bug reports do not need
adb.

## Tests and CI

`scripts/test-all.ps1` runs the JDK-only tests: charge-limit policy, update
version checks, bypass controller scenarios (including the issue #2 screen-off
sequence), and event log. `.github/workflows/tests.yml` runs the tests and an
unsigned Android build on every push and pull request. They cannot replace the
device checklist in [DEVICE-VALIDATION.md](DEVICE-VALIDATION.md).

## Privileged transport

`RootBridge` obtains Retroid's `PServerBinder` from `ServiceManager`, serializes
the expected two-string command array, and invokes transaction code `0`. A
normal `service call` command is not an equivalent test because it does not
construct that vendor String-array Parcel.

## Persistence

The desired state, separation mode, stop level, charge-again level, and boot
preference are stored in private SharedPreferences. A saved 1.5.0-rc1 resume
margin is migrated to an equivalent charge-again level when first read.
The main switch commits its desired state before starting the service. After a
process death, Android's sticky foreground-service restart reads that saved
state. If Retroid's native control is already in separation mode, the service
adopts it and reconstructs automatic-limit hysteresis from the native reading;
it does not reset a stopped-at-80% session to normal charging merely because
its in-memory flag was lost. Reopening the app reads the native control before
showing the state, and an OFF preference restores a leftover native limit.

Three consecutive samples below -300 mA while separated still trigger the
existing safety shutdown: normal charging is restored, desired state is set
OFF, and the reason is retained for the dashboard. This is a deliberate safety
stop, distinct from a process death. A normal service teardown also restores
normal charging. Removing the task from Recents only produces a diagnostic log;
it does not disable the foreground service. No receiver or alarm tries to
restart the app after Android Settings -> Force stop.
`BootReceiver` starts the service after a normal boot only when boot restoration
was explicitly enabled. Android Settings -> Force stop suppresses this behavior
until the app is opened again.
