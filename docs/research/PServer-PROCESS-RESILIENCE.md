# PServer process-resilience research

Status: **research only**  
Branch: `research/pserver-process-resilience`

This workstream does not change release behavior, the charging state machine, charging
sysfs writes, boot behavior, or the Android manifest.

## Research question

Issue #2 showed that on Flip 2 12 GB firmware `.311`, the existing controller works
correctly when the Android service remains alive, but Retroid can terminate that process
unless one of its vendor process-protection controls is used.

The first question is therefore:

> Can a bounded process launched through Retroid's existing `PServerBinder` survive the
> vendor cleaner independently of the Android app process?

That must be answered before giving any privileged helper charging authority.

## Evidence ledger

### PROVEN in this project

- The app uses `PServerBinder` transaction code `0` with
  `String[]{command, "0"}`.
- That bridge already performs released charging-control reads/writes.
- `BypassController` is Android-free and host-tested.
- On the issue #2 Flip 2 `.311`, either Retroid **Whitelist Application** or
  **Clean process when standby -> Ignored packages** independently kept the existing
  service alive long enough to stop at 80%.
- Whitelist Application also preserved foreground-notification behavior in that test.

### PROVEN in the Thor project, not Retroid

The Thor project can use the same PServer wire contract to launch root `app_process`
code from the installed APK.

That proves the mechanism is viable on the related vendor bridge. It does not prove
Flip 2 cleaner behavior.

### External implementation evidence

Public AYN/Retroid projects show long-lived PServer-launched helpers are a real pattern.
GameNative uses a detached root babysitter for crash recovery.

External implementations disagree on launcher details. One Thor-side reverse-engineering
effort reports that `setsid app_process` can fail and that inline `&`/redirection can be
unreliable through pservice.

Those observations are useful design input, not Retroid proof.

### UNTESTED on our Retroid hardware

- survival on local Flip 2 `.130`;
- survival on reporter Flip 2 12 GB `.311`;
- individual Recents swipe;
- Retroid Clear All;
- standby cleaner;
- Android Force Stop;
- Retroid cgroup placement;
- deep-sleep scheduling;
- package replacement/uninstall behavior.

## Research probe

The branch contains:

- `ProcessSurvivalProbeCommand`: pure command/script builder;
- `ProcessSurvivalProbe`: bounded root heartbeat;
- `ProcessSurvivalProbeTool`: manual adb/app_process entry point;
- `LinuxProcessIdentity`: host-tested `/proc/<pid>/stat` identity parser;
- `RestoreOnlyWatchdogPolicy`: pure fail-open policy model;
- `RestoreWatchdogSentinelScript`: harmless restore-required sentinel;
- `scripts/device-process-survival-probe.ps1`: device harness.

The probe:

- has a 60-1800 second lease;
- defaults to 10 minutes;
- writes only under `/data/local/tmp/jesty-rp-process-survival*`;
- records PID, UID, SELinux context, boot ID, cgroup, OOM score, process group/session,
  starttime ticks and heartbeat gaps;
- has a single-instance lock;
- has an explicit stop marker;
- is not in the manifest or normal UI;
- does not read/write charging sysfs;
- does not execute `settings put`.

## Launcher hardening

The initial research launcher used a long inline PServer command with `nohup + setsid`.
That was deliberately replaced before device testing.

Current flow:

```text
ADB stages fixed launcher script in /data/local/tmp
        |
        v
PServer receives only:
sh /data/local/tmp/jesty-rp-process-survival-launch.sh
        |
        v
script launches bounded nohup app_process
        |
        +-- no setsid
        +-- one-second parent grace
        +-- verifies child still exists before launcher exits
```

This isolates the physical experiment from vendor shell-parser quirks.

## Host validation

Current host tests:

```text
Process survival probe command tests passed: 3
Restore-only watchdog policy tests passed: 5
Linux process identity tests passed: 4
Restore watchdog sentinel script tests passed: 3
```

The harmless sentinel was also exercised against a disposable Linux owner process:

```text
owner alive  -> no restore marker
owner killed -> RESTORE_REQUIRED owner_missing
```

Classification: **HOST-PROVEN, DEVICE-UNTESTED**.

## Physical lifecycle protocol

Keep charging separation **OFF** for the whole survival test.

### Preconditions

- remove Jesty RP Charging Separation from Retroid Whitelist Application;
- remove it from Clean process when standby -> Ignored packages;
- connect authorized ADB;
- install a research APK from this branch.

### Start clean

```powershell
.\scripts\device-process-survival-probe.ps1 -Action Clean
.\scripts\device-process-survival-probe.ps1 -Action Start -DurationSeconds 600
```

Then:

```powershell
.\scripts\device-process-survival-probe.ps1 -Action Status
```

Required before continuing:

- `START probe_version=2`;
- `uid=0`;
- several heartbeats;
- fixed PID;
- `PROCESS ... start_ticks=...`;
- `boot_id=...`;
- cgroup information.

### Single-variable checks

Capture status before and after each action:

1. close the dashboard normally;
2. swipe only the app from Recents;
3. Retroid Clear All;
4. standby cleaner;
5. Android Force Stop last.

**PROVEN SURVIVAL** requires the same PID/starttime identity and a continuing heartbeat
sequence with no new START line.

A large `gap_ms` means lifecycle survived but scheduling paused. That distinction is
important.

### Stop

```powershell
.\scripts\device-process-survival-probe.ps1 -Action Stop
.\scripts\device-process-survival-probe.ps1 -Action Status
```

Expected final event:

```text
END reason=stop_requested
```

The lease is a backstop if explicit stop is forgotten.

## Current architecture decision

Do **not** move the full charging controller into root at this stage.

Preferred layering:

```text
Retroid vendor process protection
        |
        v
Android foreground service
  BypassController remains policy owner
        |
        +-- threshold/hysteresis
        +-- retries
        +-- wake lock
        +-- safety monitoring
        |
        v
future restore-only root watchdog
  may restore normal charging
  may never enable separation
```

Why:

- the `.311` evidence shows current charging logic works when alive;
- vendor process protection fixes the demonstrated failure;
- a full root controller adds much more privileged state than the evidence justifies;
- the remaining independent safety gap is abrupt death while native separation is active.

See `PROCESS-RESILIENCE-DECISION.md` and `RESTORE-ONLY-WATCHDOG-DESIGN.md`.

## Next gates

Before any watchdog receives charging authority:

1. prove detached survival on Retroid;
2. measure screen-off/deep-sleep heartbeat gaps;
3. prove exact PID/starttime identity on-device;
4. run the harmless sentinel on-device;
5. test Force Stop and package update behavior;
6. define restore-write retry/failure policy;
7. only then consider a restore-only charging prototype.

No production charging daemon is justified by current evidence.
