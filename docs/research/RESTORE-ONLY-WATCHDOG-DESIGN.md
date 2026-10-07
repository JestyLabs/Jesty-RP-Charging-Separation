# Restore-only watchdog design

Status: **design + host-tested policy only; no charging watchdog implementation**

## Design choice

If a privileged safety backstop is added, prefer a **small shell watchdog** over moving
the full `BypassController` into a root Java daemon.

The watchdog has one authority:

> restore normal charging and exit.

It must never enable charging separation.

## Why shell is attractive

A restore-only watchdog needs no UI, Android framework callbacks, SharedPreferences,
threshold logic or long-lived app IPC.

A shell process also avoids coupling the safety backstop to the APK/classloader after:

- package replacement;
- APK path changes;
- UI process death;
- application code refactors.

The existing Android service remains the owner of all charging policy.

## Strong owner identity

Checking only `kill -0 PID` is insufficient because Linux can reuse a PID.

A future watchdog owner token should contain at least:

```text
boot_id
pid
/proc/<pid>/stat starttime (field 22)
```

The research branch includes a host-tested `LinuxProcessIdentity` parser that rejects a
same-PID/different-starttime process.

A lease heartbeat is an additional liveness signal, not a substitute for exact identity.

## Safe activation ordering

The important race is process death between applying native separation and arming the
watchdog.

Preferred ordering:

```text
1. user intent already says ON
2. launch watchdog with exact owner identity
3. prove watchdog is alive and watching
4. apply native separation
5. verify native readback/status
6. publish ACTIVE
```

If the owner dies after step 3 but before step 4, the watchdog sees normal charging and
exits without a charging write.

Do **not** use:

```text
apply separation -> later launch watchdog
```

because death in that gap leaves unmonitored separation active.

## Safe disable ordering

Preferred clean shutdown:

```text
1. restore native charging to 0
2. verify readback 0
3. retire watchdog
4. publish OFF / stop service
```

Never retire the watchdog before verified restoration.

The host-tested `RestoreOnlyWatchdogPolicy` encodes this rule: a retire request while
native separation is still active resolves to `RESTORE_NORMAL_THEN_EXIT`, not `EXIT`.

## Owner death / stale lease policy

| Owner identity | Lease | Native separation | Action |
|---|---|---|---|
| valid | fresh | either | keep watching |
| dead/mismatched | any | active | restore normal, exit |
| dead/mismatched | any | normal | exit |
| valid | stale | active | restore normal, exit |
| valid | stale | normal | exit |
| retire requested | any | active | restore normal, exit |
| retire requested | any | normal | exit |

There is intentionally no action that enables separation.

## Unplugged ARMED nuance

Today the controller may transition from ACTIVE to ARMED after USB removal while the
native limit remains non-zero. That lets a later replug reattach cleanly.

Therefore a future watchdog cannot simply be tied to the in-memory `ACTIVE` enum. It
must protect the period in which a non-zero native separation request may persist,
including an unplugged/armed interval.

If the app dies while unplugged, restoring the native limit to `0` is conservative and
prevents an unmonitored separation from reappearing on the next plug.

## Restore failure

A watchdog must never report success merely because a shell command returned.

Required behavior:

1. write normal limit;
2. read back the node;
3. only exit normally after readback is `0`.

The retry/backoff policy for a failed restore is still **UNRESOLVED**. It should be
designed separately because two requirements conflict:

- do not abandon an unsafe/unmonitored state after one failed write;
- do not leave an orphan privileged process running forever.

No production watchdog should be implemented until this failure policy is explicit and
physically tested.

## Production gate

Before any charging write is added to a watchdog:

1. process-survival probe must pass on Retroid hardware;
2. screen-off scheduling behavior must be measured;
3. exact owner identity must be validated on-device;
4. restore-only ordering must be exercised against a harmless sentinel first;
5. package update and Force Stop behavior must be tested;
6. only then may a charging restore prototype be considered.
