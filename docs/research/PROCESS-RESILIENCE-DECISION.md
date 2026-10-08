# Process-resilience architecture decision

Status: **research decision, not production implementation**

## Cross-device evidence update (2026-10-08)

[Verified Thor / Flip 2 comparison](PSERVER-CROSS-DEVICE-ASSESSMENT.md) establishes
a common Binder/command core, with materially different startup and CPU/device
management. It does not establish OEM cleaner survival or charging safety.
The decision remains unchanged: Android owns charging policy; a future helper is
restore-only and physically gated. Next discriminant is a harmless leased sentinel
through Retroid Clear All, with separation OFF and exact identity/heartbeat tracking.
Do not reuse Thor's boot_start hook or infer helper success from transact acceptance.

## Decision

Do **not** move the full charging controller into a privileged root daemon yet.

The preferred long-term architecture is layered:

1. **Retroid process protection** keeps the normal Android service alive.
2. **BypassController remains the single charging-policy implementation** inside the
   Android service.
3. A **small detached root safety watchdog** is considered only as a fail-safe for
   abrupt process death while native separation is active.
4. A full privileged controller is a fallback design only if physical testing proves
   that Retroid process protection cannot be made reliable enough.

## Why

Issue #2 gives unusually clean evidence on Flip 2 12 GB firmware `.311`:

- v1.5.9 reaches the configured 80% stop level correctly when the service remains alive.
- Either Retroid **Whitelist Application** or **Clean process when standby -> Ignored
  packages** independently keeps the service alive long enough for the 80% transition.
- Whitelist Application additionally preserves foreground-notification behavior.

That means the charging state machine itself is not the demonstrated failure. The
demonstrated failure is lifecycle ownership.

Moving the whole controller to uid 0 would solve lifecycle ownership by greatly
increasing the privileged state and orphan-process surface. That is not justified while
Retroid already exposes a vendor mechanism that fixes the demonstrated failure.

## Current abrupt-death failure modes

### Process dies in CHARGING_TO_LIMIT

Native limit is still normal charging (`limit=0`).

Result:
- battery continues charging;
- configured stop level is missed;
- feature fails open to normal charging.

This is the issue #2 behavior class. It is undesirable but electrically conservative.

### Process dies in ACTIVE

Native separation has already been applied (`limit=limitMax`).

Result:
- separation can remain active;
- Android safety monitoring is gone;
- repeated negative battery-current safety samples can no longer trigger restoration.

This is the more important safety reason to investigate an independent watchdog.

### Normal service destruction

`BypassController.destroy()` attempts restoration from ACTIVE/ENABLING, so the concern
is specifically **abrupt death where teardown never runs**.

## Preferred layered design

```text
Retroid vendor process protection
          |
          v
Android foreground service
  BypassController
  - thresholds / hysteresis
  - retries
  - USB + battery events
  - charge-to-limit wake lock
  - battery-drain safety
          |
          | when native separation becomes ACTIVE
          v
detached root safety watchdog
  - watches exact owning app PID/session
  - has one closed purpose: restore normal charging if owner dies
  - exits after restore
  - no threshold controller
  - no UI
  - no arbitrary command IPC
```

## Why a safety watchdog is preferable to a full root controller

A watchdog can be made almost stateless.

It does not need:
- 80/70 threshold logic;
- user preferences;
- notification state;
- USB event handling;
- long-lived IPC;
- config migration;
- retries that can re-enable separation;
- ownership after the app dies.

Its authority is intentionally asymmetric:

> It may restore normal charging. It may never enable charging separation.

That property removes a large class of stale-daemon and explicit-OFF races.

## Watchdog safety invariants

A production watchdog must satisfy all of these before integration:

1. It is armed only after separation has been verified ACTIVE.
2. Its only charging mutation is restoration to normal charging.
3. It monitors an exact owner identity, not just a package-name substring.
4. Owner death causes one restore attempt and then watchdog exit.
5. Normal disable restores charging before watchdog retirement; duplicate restore is
   idempotent and safe.
6. Package replacement/process restart may cause a brief fail-open restoration; a fresh
   service may re-enable later if user intent still says ON.
7. Force Stop must never leave native separation active without monitoring.
8. No watchdog process may survive indefinitely: owner-death exit plus a maximum lease.
9. Watchdog identity must be unique enough that cleanup cannot kill unrelated root
   processes.
10. A broken watchdog must not block normal charging restoration from the Android path.

## Role of PREF_REQUESTED

The current `requested` preference is written by the controller but is not yet a robust
ownership journal.

Do not promote it to watchdog authority without defining:
- write ordering relative to native sysfs writes;
- crash consistency;
- startup reconciliation;
- OFF ordering;
- stale value cleanup;
- package-update behavior.

For a first watchdog prototype, runtime owner PID + verified native state is a safer
source than treating `requested=true` as proof.

## When a full root controller becomes justified

Reconsider a privileged ChargingDaemon only if at least one of these is proven:

- Retroid Whitelist Application cannot be detected/configured reliably across supported
  firmware;
- the cleaner still terminates the app despite verified vendor protection;
- Android lifecycle restrictions remain capable of breaking threshold monitoring after
  process protection is correct;
- a root scheduling/wake probe demonstrates materially better reliability with a
  bounded, auditable runtime.

Even then, reuse `BypassController`; do not fork the policy logic.

## Next physical gates

1. Prove the exact Retroid Whitelist Application backing store with OFF -> ON -> OFF.
2. Run the detached PServer process-survival probe with charging separation OFF.
3. If it survives the relevant cleaner, measure screen-off scheduling gaps.
4. Only then prototype a restore-only watchdog.
5. Only after watchdog proof consider automatic process-protection enrollment.
