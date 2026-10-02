# Architecture

## UI process

`MainActivity` owns only the dashboard, user switches, and one-second telemetry
refresh while visible. The live dashboard stays in a right-hand panel on wide
landscape screens and is stacked under the controls on narrower screens.
Closing the Activity does not represent the controller's requested state.

## Foreground controller

`BypassService` is the state machine:

```text
OFF -> ENABLING -> ACTIVE
               -> CHARGING_TO_LIMIT (automatic limit, below limit)
               -> ARMED (no USB)
               -> FAILED (restore attempted)

ACTIVE <-> CHARGING_TO_LIMIT (automatic limit hysteresis)
```

In **Right away** mode the service separates as soon as USB is present. In
**At a battery level** mode it keeps normal charging until the configured stop
level (default 80%), separates, and restores normal charging at the charge-again
level (default 70%, always at least five points below the stop level).

It validates the native controls, performs privileged writes, confirms
readback/status, and samples telemetry every two seconds while active, charging
to the limit, or armed.

## Privileged transport

`RootBridge` obtains Retroid's `PServerBinder` from `ServiceManager`, serializes
the expected two-string command array, and invokes transaction code `0`. A
normal `service call` command is not an equivalent test because it does not
construct that vendor String-array Parcel.

## Persistence

The desired state, separation mode, stop level, charge-again level, and boot
preference are stored in private SharedPreferences. A saved 1.5.0-rc1 resume
margin is migrated to an equivalent charge-again level when first read.
`BootReceiver` starts the service after a normal boot only when boot restoration
was explicitly enabled. Android Settings -> Force stop suppresses this behavior
until the app is opened again.
