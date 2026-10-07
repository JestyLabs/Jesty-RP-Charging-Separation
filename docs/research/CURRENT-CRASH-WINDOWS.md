# Current controller crash / teardown windows

Status: **characterization of current main behavior; no behavior change in this branch**

Source of truth: current `BypassController` on `main`.

## State-by-state teardown surface

| State | Expected native limit before abrupt death | `destroy()` restores? | Notes |
|---|---:|---|---|
| OFF | 0 | no | already normal |
| ARMED from initial unplugged start | 0 | no | normal |
| ARMED after unplug from ACTIVE | may remain `limitMax` | **no** | important window |
| ENABLING | 0 or transitional | yes | `destroy()` calls `disable()` |
| ACTIVE | `limitMax` | yes | graceful teardown restores |
| CHARGING_TO_LIMIT | 0 | no | normal charging is intentionally active |
| RETRYING | 0 | no | retry path restores before entering state |
| FAILED | intended 0, but restore may itself have failed | no | failure reason is preserved |

Abrupt process death can of course bypass `destroy()` entirely.

## Confirmed ARMED window

Current `monitorActive()` behavior on USB removal is:

```text
ACTIVE
  native limit = limitMax
  requested = true
      |
      | USB removed
      v
ARMED
  native limit is not restored in this transition
  requested remains true
```

Current `destroy()` restores only when state is `ACTIVE` or `ENABLING`.

Therefore a graceful service teardown that occurs after the ACTIVE -> ARMED transition
does not currently restore the non-zero native limit.

The research branch adds `BypassCrashWindowTest` to pin this current behavior without
changing it.

## Reconciliation that already exists

If a new controller process later starts while USB is still unplugged:

```text
start(desired=true)
  -> enable()
  -> sees USB absent
  -> arm(before)
  -> non-zero limit => restoreAndConfirm()
  -> ARMED with limit 0
```

So a successful sticky/app restart repairs the persisted native limit.

Explicit user OFF also calls `disable()` and restores normal charging.

The unresolved case is not "can a restart recover?" It can. The concern is:

> what happens when the process does not restart before the next charger interaction?

That is exactly the reliability class exposed by issue #2 on firmware `.311`.

## Why this matters to the watchdog design

A future safety watchdog cannot be armed only while the Java state enum is `ACTIVE`.
The potentially non-zero native request can outlive ACTIVE during an unplugged ARMED
interval.

A restore-only watchdog should therefore be tied to **verified ownership of a non-zero
native separation request**, not merely to one in-memory state name.

If ownership disappears while USB is unplugged, restoring `0` is conservative: normal
charging on the next plug is safer than allowing an unmonitored separation request to
reappear.

## What is proven vs not

### PROVEN FROM CURRENT CODE / HOST CHARACTERIZATION

- ACTIVE -> unplug transitions to ARMED without calling `restoreAndConfirm()`.
- current `destroy()` does not restore from ARMED.
- a later restart while unplugged calls `arm()` and restores a non-zero limit.
- explicit OFF restores from ARMED.

### INFERRED

- if the Retroid kernel/vendor control preserves the non-zero native limit across unplug,
  a later USB reconnect before process recovery may re-enter native separation without
  Java monitoring.

The code strongly motivates testing this, but the reconnect effect itself still needs
physical Retroid evidence.

## No fix in this research branch

A tempting change would be to make `destroy()` restore from ARMED as well. Do not make
that change inside the process-resilience research PR.

Reasons:

- it changes released charging behavior;
- it does not solve abrupt kills where `onDestroy()` never runs;
- the current preservation across unplug may have intentional reconnect semantics;
- physical device evidence should establish the exact native behavior first.

Treat it as a separate candidate fix after the read-only / lifecycle work is complete.
