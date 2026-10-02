# Architecture

## UI process

`MainActivity` owns only the dashboard, user switches, and one-second telemetry
refresh while visible. Closing the Activity does not represent the controller's
requested state.

## Foreground controller

`BypassService` is the state machine:

```text
OFF -> ENABLING -> ACTIVE
               -> CHARGING_TO_LIMIT (automatic limit, below limit)
               -> ARMED (no USB)
               -> FAILED (restore attempted)

ACTIVE <-> CHARGING_TO_LIMIT (automatic limit hysteresis)
```

In **Immediate** mode the service separates as soon as USB is present. In
**Automatic limit** mode it keeps normal charging until the battery reaches the
configured limit (default 80%), separates, and restores normal charging once the
battery falls to `limit - margin` (default margin 10 points, i.e. 70%).

It validates the native controls, performs privileged writes, confirms
readback/status, and samples telemetry every two seconds while active, charging
to the limit, or armed.

## Privileged transport

`RootBridge` obtains Retroid's `PServerBinder` from `ServiceManager`, serializes
the expected two-string command array, and invokes transaction code `0`. A
normal `service call` command is not an equivalent test because it does not
construct that vendor String-array Parcel.

## Persistence

The desired state, separation mode, limit, resume margin, and boot preference are stored in private SharedPreferences.
`BootReceiver` starts the service after a normal boot only when boot restoration
was explicitly enabled. Android Settings -> Force stop suppresses this behavior
until the app is opened again.
