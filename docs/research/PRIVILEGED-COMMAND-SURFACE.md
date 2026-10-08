# Privileged command surface audit

Status: **research audit**

## Current production charging path

The existing privileged bridge is package-private `RootBridge.exec(String)`.

Current charging mutations in `BypassService.SysfsHardware` are:

```text
chmod 644 /sys/class/power_supply/battery/charge_control_limit
echo <int> > /sys/class/power_supply/battery/charge_control_limit
chmod 444 /sys/class/power_supply/battery/charge_control_limit
```

Readback is:

```text
cat /sys/class/power_supply/battery/charge_control_limit
```

### Input analysis

- The sysfs path is a compile-time constant.
- The value passed to `writeLimit(int)` is numeric.
- The state machine currently writes either restoration `0` or the numeric native
  `limitMax` it read from telemetry.
- No user-provided string is interpolated into these shell commands.

**Finding:** no command-injection path was identified in the current charging-control
surface.

This does not prove the generic `RootBridge.exec(String)` API is intrinsically safe;
it means its current callers are narrow.

## PR #5 read-only diagnostic surface

`RetroidProcessProtection` issues one constant privileged read:

```text
settings list system
```

The package name is used only for local parsing/token matching. It is not interpolated
into the root command.

The new ADB capture harness reads:

- `settings list system`
- `settings list global`
- `settings list secure`
- `settings get system app_whiteList`
- `cmd deviceidle whitelist`
- selected build properties

A static guard rejects mutation patterns such as `settings put/delete/reset`,
device-idle additions/removals, `am force-stop`, `setprop`, package mutation and
charging-control writes.

## PR #9 research surface

The process-survival research sends only fixed-path commands through PServer:

- `sh /data/local/tmp/jesty-rp-process-survival-launch.sh`
- `tail` the fixed log
- `touch` the fixed stop marker
- `rm -f` fixed research artifacts

The launcher script is staged by ADB and has a bounded lease. It does not access
charging sysfs or Settings.

## Structural risk

`RootBridge.exec(String)` is an arbitrary-command primitive. Any future package-private
caller can expand the privileged surface accidentally.

A long-term refactor should not expose generic shell to charging policy or UI code.
Preferred layering:

```text
PServerTransport              raw binder mechanics, smallest visibility possible
  |
  +-- RetroidChargeHardware   fixed read/write operations only
  +-- VendorPolicyReader      fixed read-only settings operations
  +-- ProcessLauncher         fixed research/daemon bootstrap operations
```

The policy/controller should depend on typed operations, not command strings.

## Concurrency

`BypassService` uses a single worker, which serializes its own PServer activity.
`RootBridge` itself is not process-wide synchronized, so diagnostics or other callers
could overlap a service transaction.

Concurrent callers are a design consideration; no project failure has established
that overlapping transactions caused an incident.

No current Charging Separation bug is attributed to this, so do not change the release
bridge as part of PR #5 or PR #9. A future transport refactor should use one process-wide
transaction lock and reacquire a dead Binder.
