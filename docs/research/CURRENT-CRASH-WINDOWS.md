# Controller process-death windows

Status: research characterization reconciled with main after PR #11. No controller changes in PR #9.

## Historical unplug window and current behavior

Before PR #11, ACTIVE -> unplug could enter ARMED with the native limit still nonzero and requested still true. That historical characterization motivated investigation; it is no longer the current expected behavior.

Current monitorActive() calls arm(telemetry) on unplug. arm() restores and confirms limit 0 when needed, clears requested, releases the wake lock and only then enters ARMED. The saved desired user choice remains enabled for reconnect. destroy() need not repeat the restore after a successful ARMED transition. PR #11 records focused physical restore/reconnect results; those are separate from the research helper evidence.

BypassCrashWindowTest now asserts restored limit 0, cleared requested, preserved desired choice and safe ARMED teardown. It also models a distinct abrupt death while ACTIVE: no unplug tick or destroy callback executes, the prior native request can remain until reconciliation, and a later controller start while unplugged restores normal charging. Explicit OFF also restores normal charging.

## Remaining concern

Abrupt process death can bypass all graceful callbacks. A successful later restart demonstrates reconciliation, not timely recovery when Android does not restart the process. Do not equate the corrected unplug path with this remaining owner-loss case or claim that the historical reconnect bug remains present.

## Research boundary

The Android controller remains the sole policy owner. Any future helper would be restore-only, require exact owner identity and verified native readback, and need a separate production proposal. This PR introduces no charging writes, controller changes or automatic watchdog. See RETROID-RESEARCH-VALIDATION-MATRIX.md for pending physical experiments.
