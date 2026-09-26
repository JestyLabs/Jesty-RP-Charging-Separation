# Live validation status

## Confirmed foundations

- Retroid's `PServerBinder` is reachable through the hidden Android service
  manager on the original development hardware.
- The vendor transaction expects a two-element String array and transaction code
  `0`.
- Capability checks replace the old model-name whitelist.
- The RP5 inspection observed readable native limit nodes with a `0/10` state.

## Not yet confirmed for this public build

- The renamed package and final artwork have not yet been installed on hardware.
- RP5 end-to-end charging separation and real power flow remain unverified.
- Reboot, unplug/replug, service-destruction, and restore behavior must be
  repeated after the final build is signed.

No pending item should be described as passed until a real-device run records
the final state.
