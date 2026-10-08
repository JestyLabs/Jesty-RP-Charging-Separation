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

## Corrected unplug path and abrupt owner loss

PR #11 restores and confirms normal charging before entering ARMED on unplug. The historical nonzero ARMED window is no longer the baseline. A future watchdog must instead address verified loss of ownership of an active native request, without assuming callbacks execute after abrupt death.

## Restore failure

A watchdog must never report success merely because a shell command returned.

Required behavior:

1. write normal limit;
2. read back the node;
3. only exit normally after readback is `0`.

The host-tested `RestoreRetryPolicy` now resolves the policy side of this conflict:

```text
restore attempt -> readback 0      -> exit
restore attempt -> no verified 0   -> retry
```

Backoff is bounded:

```text
1 s -> 2 s -> 5 s -> 10 s -> 30 s -> 60 s -> 60 s ...
```

Once owner loss or stale ownership has made restoration mandatory, the original watchdog
lease no longer authorizes abandoning the restore. The helper may exit only after normal
charging is verified.

This deliberately chooses a sleeping restore-only process retrying once per minute over
leaving a known unmonitored separation behind. Because the helper has no authority to
enable separation, its orphan risk is much smaller than that of a full charging daemon;
a reboot remains a natural hard lifetime boundary.

**Policy is resolved; device behavior is not.** Actual restore writes, readback failure
modes and long-lived retry behavior still require physical validation before production.

## Production gate

Before any charging write is added to a watchdog:

1. process-survival probe must pass on Retroid hardware;
2. screen-off scheduling behavior must be measured;
3. exact owner identity must be validated on-device;
4. restore-only ordering must be exercised against a harmless sentinel first;
5. package update and Force Stop behavior must be tested;
6. only then may a charging restore prototype be considered.


## Host validation completed

The harmless sentinel version was exercised on a Linux host with a disposable owner
process. This paragraph describes host evidence only; the later bounded Flip 2 owner-loss sentinel observation is recorded in PServer-PROCESS-RESILIENCE.md. It did not perform charging restoration.

Observed host sequences:

```text
self-detach launcher returned in ~1.0 s
owner alive      -> sentinel did not fire
owner killed     -> RESTORE_REQUIRED owner_missing
explicit Stop    -> SENTINEL_END stop_requested
```

The research host-test surface is now:

```text
Process survival probe command tests passed: 3
Restore-only watchdog policy tests passed: 5
Linux process identity tests passed: 4
Restore watchdog sentinel script tests passed: 4
Restore retry policy tests passed: 2
```

That is 18 JDK policy/command checks, plus the static no-charging/no-vendor-policy guard.

During this work a shell bug was caught before device use: positional field 20 must be
addressed as `${20}`, not `$20`, otherwise POSIX shell can parse it as `$2` followed
by `0`. The test now pins the correct syntax.

This host result proves only the ownership/sentinel logic under a normal Linux shell. It
does not prove Android toybox behavior, PServer launch behavior, Retroid cgroups, or
screen-off scheduling.


## Harmless Retroid sentinel protocol

After the basic detached survival probe has proven that PServer can keep a helper alive,
the branch contains a second device harness that still has **zero charging authority**:

```powershell
.\scripts\device-watchdog-sentinel.ps1 -Serial $Serial -ResearchApk $ResearchApk -Action Start -LeaseSeconds 30
.\scripts\device-watchdog-sentinel.ps1 -Serial $Serial -ResearchApk $ResearchApk -Action Status
```

It captures the currently running app's exact PID + `/proc/PID/stat` starttime, stages
a fixed shell script, and asks PServer to run only:

```text
sh /data/local/tmp/jesty-rp-watchdog-sentinel.sh
```

The script self-detaches its worker and then watches the owner identity. Expected
research outcomes are only:

```text
RESTORE_REQUIRED owner_missing
RESTORE_REQUIRED owner_mismatch
RESTORE_REQUIRED lease_expired
SENTINEL_END stop_requested
```

None of those performs a restore; they only show what a future watchdog **would** have
decided.

Clean termination:

```powershell
.\scripts\device-watchdog-sentinel.ps1 -Serial $Serial -ResearchApk $ResearchApk -Action Stop
.\scripts\device-watchdog-sentinel.ps1 -Serial $Serial -ResearchApk $ResearchApk -Action Status
.\scripts\device-watchdog-sentinel.ps1 -Serial $Serial -ResearchApk $ResearchApk -Action Clean
```

Do not run this before the simpler survival probe has established the PServer launch
behavior on the device.

## Lease boundary

The current harmless sentinel has a fixed bounded lease and no renewal API. Expiry emits a research marker and exits; it cannot restore charging. This is intentionally different from a future production helper. Before any production prototype, separately specify authenticated renewal, monotonic expiry, exact boot/PID/starttime ownership and restore/readback behavior. The host retry model does not establish physical recovery safety.
