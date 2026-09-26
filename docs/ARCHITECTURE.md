# Architecture

## UI process

`MainActivity` owns only the dashboard, user switches, and one-second telemetry
refresh while visible. Closing the Activity does not represent the controller's
requested state.

## Foreground controller

`BypassService` is the state machine:

```text
OFF -> ENABLING -> ACTIVE
               -> ARMED (no USB)
               -> FAILED (restore attempted)
```

It validates the native controls, performs privileged writes, confirms
readback/status, and samples telemetry every two seconds while active or armed.

## Privileged transport

`RootBridge` obtains Retroid's `PServerBinder` from `ServiceManager`, serializes
the expected two-string command array, and invokes transaction code `0`. A
normal `service call` command is not an equivalent test because it does not
construct that vendor String-array Parcel.

## Persistence

The desired state and boot preference are stored in private SharedPreferences.
`BootReceiver` starts the service after a normal boot only when boot restoration
was explicitly enabled. Android Settings -> Force stop suppresses this behavior
until the app is opened again.
