# PServer process-resilience research

Status: **research only**  
Branch: `research/pserver-process-resilience`

This workstream does not change release behavior, the charging state machine, charging sysfs
writes, boot behavior, or the Android manifest. It exists to answer whether a small,
bounded privileged process launched through Retroid's existing `PServerBinder` can
survive the vendor process cleaner that terminates the normal app process.

## Why investigate this

Issue #2 on the Flip 2 12 GB `.311` firmware showed that the auto-limit controller can
stop receiving events because the whole Android process is terminated. The Retroid
**Whitelist Application** and **Clean process when standby -> Ignored packages** controls
each independently kept the controller alive in the reporter's physical tests.

The current controller already has a useful architectural property:
`BypassController` has no Android dependencies. Android lifecycle, notifications,
wake locks and PServer/sysfs access are adapters around it. That means a future
process-resilient runtime could potentially reuse the same state machine rather than
duplicating the 80/70 hysteresis and safety logic.

The research question is deliberately narrower:

> Can PServer launch a process whose lifetime is independent of the app UID/process on
> the affected Retroid firmware?

Until that is physically proven, moving charging control into a daemon is only a design
option, not a planned fix.

## Evidence ledger

### PROVEN in this project

- The app can transact with `PServerBinder` using transaction code 0 and
  `String[]{command, "0"}`.
- That bridge already performs the native charging-control reads/writes used by v1.5.9.
- `BypassController` is Android-free and host-tested.
- The controller can adopt an already-active native separation after a normal process
  restart instead of blindly rewriting it.
- On the issue #2 Flip 2 `.311`, either Retroid vendor protection mechanism was enough
  to keep the existing Android service/controller alive during the reporter's charging
  test; Whitelist Application also preserved the foreground notification.

### PROVEN in the Thor project, not yet on Retroid

The Thor project uses the same PServer wire contract to launch a root `app_process`
from the installed APK. That proves the mechanism is technically viable on the shared
AYN/Retroid-style bridge, but it does not prove Retroid cleaner behavior.

### External implementation evidence

GameNative's PServer driver targets AYN/Retroid-class devices and implements a detached
root "babysitter" using `nohup` + `setsid`. Its stated purpose is to survive the app
process and restore a persisted power baseline when that app dies.

That is useful corroboration that detached children are a real PServer use case. It is
**not** accepted as proof that our Flip 2 cleaner leaves such a process alive.

### INFERRED

- A root process in a separate session is likely outside the normal app-UID process-kill
  path used by Retroid's cleaner.
- If so, a small privileged controller could preserve threshold/safety monitoring even
  when the UI/service process is killed.

### UNTESTED

- Survival on the maintainer Flip 2 `.130`.
- Survival on the reporter Flip 2 12 GB `.311`.
- Survival after an individual Recents swipe.
- Survival after Retroid "Clear all".
- Survival after standby cleaner action.
- Survival after Android Force Stop.
- Whether the detached process is placed in a cgroup that the Retroid cleaner also kills.
- Whether a detached root process can maintain the required scheduling/wake behavior
  during screen-off/deep sleep.
- Safe package update/uninstall handling.
- Safe authenticated app <-> daemon IPC on Retroid.
- Any production migration of charging control.

## Research probe

The branch contains a probe with a deliberately tiny capability surface:

- `ProcessSurvivalProbeCommand`: pure command builder.
- `ProcessSurvivalProbe`: bounded root `app_process` heartbeat.
- `ProcessSurvivalProbeTool`: manual `adb/app_process` entry point that calls the
  existing `RootBridge`.
- `scripts/device-process-survival-probe.ps1`: convenience harness.

The probe:

- has a 60-1800 second lease;
- defaults to 10 minutes;
- writes only under `/data/local/tmp/jesty-rp-process-survival*`;
- records PID, UID, SELinux context, cgroup membership and heartbeat gaps;
- has an explicit stop marker;
- has a single-instance file lock;
- is not registered in the manifest;
- is not reachable from the normal app UI;
- does not read or write charging sysfs;
- does not execute `settings put`;
- does not change Retroid whitelist/cleaner settings.

Host tests fail if the command builder gains charging-control paths or Settings writes.

## Physical test protocol

Use a research APK built from this branch. Keep charging separation **OFF** for the
entire test.

### 0. Preconditions

- Disable Jesty RP Charging Separation in Retroid **Whitelist Application**.
- Remove it from **Clean process when standby -> Ignored packages**.
- Confirm the device is visible in `adb devices`.

### 1. Clean previous probe artifacts

```powershell
.\scripts\device-process-survival-probe.ps1 -Action Clean
```

### 2. Start a ten-minute lease

```powershell
.\scripts\device-process-survival-probe.ps1 -Action Start -DurationSeconds 600
```

Expected submission output contains a command length and duration. This only proves that
the PServer transaction was accepted.

### 3. Prove the root child really started

Wait a few seconds:

```powershell
.\scripts\device-process-survival-probe.ps1 -Action Status
```

Required evidence before continuing:

- a `START` line;
- `uid=0`;
- multiple `HEARTBEAT` lines with the same PID;
- an `IDENTITY` line;
- a `CGROUP` line.

If `uid` is not 0, stop. The intended PServer-root mechanism has not been proven.

### 4. Single-variable survival checks

Run `Status` immediately before and after each action. Do not combine actions.

1. Close the normal app.
2. Swipe only the app from Recents.
3. Use Retroid Clear All.
4. Allow/trigger the normal standby-cleaner behavior.
5. Only after the above: Android Force Stop.

For each action, record:

- last heartbeat before;
- first heartbeat after;
- PID;
- `gap_ms`;
- whether a new `START` appeared.

### Interpretation

**PROVEN SURVIVAL** for one action requires:

- same PID before and after;
- heartbeat sequence continues;
- no new `START`;
- no evidence that the probe was relaunched.

A long `gap_ms` means the process survived but was not scheduled for that interval.
That is an important distinction: lifecycle survival alone does **not** prove it can
implement the charging threshold during deep sleep.

If the heartbeat ends exactly at the cleaner action, detached PServer ownership does not
solve issue #2 by itself.

### 5. Stop explicitly

```powershell
.\scripts\device-process-survival-probe.ps1 -Action Stop
```

Then inspect once more:

```powershell
.\scripts\device-process-survival-probe.ps1 -Action Status
```

Expected final line:

```text
END reason=stop_requested ...
```

If forgotten, the probe self-terminates when its lease expires.

## Architecture only if survival is proven

A production design should keep the existing separation between policy and platform:

```text
Android process
  UI / notification / config
          |
          | authenticated narrow IPC
          v
privileged ChargingDaemon
  BypassController
    Hardware -> direct sysfs adapter
    Settings -> versioned desired/config snapshot
    Platform -> clock, scheduling, logging, safety stop
```

The privileged process must **not** expose an arbitrary-shell IPC endpoint. The PServer
bridge is only a bootstrap mechanism.

## Required production invariants

These are stop conditions, not optional polish:

1. **Explicit OFF wins.** A stale daemon must never re-enable separation after the user
   turned it off.
2. **Lease / owner freshness.** An orphan daemon must expire to a safe state.
3. **Single instance.** Versioned lock + runtime identity; never two controllers writing
   the same node.
4. **Version handshake.** An updated APK must identify and retire an incompatible daemon
   before enabling a successor.
5. **Fail safe to normal charging.** Lost config, broken IPC, invalid telemetry or stale
   ownership must not leave unmonitored separation active.
6. **No arbitrary root command IPC.** Closed commands and validated values only.
7. **Authenticated peer.** Verify the app UID on the local socket, not merely a protocol
   string.
8. **Package replacement/uninstall story.** The daemon cannot be allowed to persist
   indefinitely after its owner disappears.
9. **Charging state machine stays single-source.** Reuse `BypassController`; do not
   fork a second implementation of hysteresis/safety logic.
10. **Physical deep-sleep proof before migration.** Surviving process cleaning is
    necessary but not sufficient.

## Next research step after lifecycle proof

If the root probe survives the relevant Retroid cleaner, the next workstream is a
**wake/scheduling probe**, still without charging writes:

- record heartbeat behavior screen-on vs screen-off;
- quantify deep-sleep scheduling gaps;
- investigate the smallest safe wake mechanism available to a root `app_process`;
- prove clean release of that wake mechanism;
- only then prototype a read-only battery telemetry loop.

No charging-control migration should happen before those two proofs are complete.
