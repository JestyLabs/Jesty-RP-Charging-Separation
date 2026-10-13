## Summary

Describe the problem and the measured behavior this change addresses.

## Validation

- [ ] Clean unsigned build completed.
- [ ] `scripts/test-all.ps1` passes, and the CI test workflow is green.
- [ ] Behavior changes to charging control include a `BypassControllerTest`
  scenario that fails without the change.
- [ ] Screen-off behavior was checked with the screen off and without
  reopening the app, when the change affects charging control.
- [ ] No secrets, personal paths, serials, APKs, or unredacted logs were added.
- [ ] `scripts/test-public-content.ps1` passes; screenshots and attachments were manually checked for private details.
- [ ] Fail-closed restoration behavior was preserved.
- [ ] Compatibility claims include real-device evidence.
- [ ] Material AI assistance was disclosed.

## Hardware evidence

List device, firmware, charger, relevant telemetry, and final restored state.
