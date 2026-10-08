# Retroid research validation matrix

Status: protocol only. No new physical result is claimed by this update.

The stable Android app remains the charging-policy owner. Research tools produce
heartbeats and markers only. They have no charging-control authority.

## Shared preparation and stop conditions

Confirm the intended Flip 2 serial/model, separation OFF and native limit 0.
Record existing vendor protection settings and preserve them. Set `$Serial`
locally and `$ResearchApk` to a separately staged APK under `/data/local/tmp`;
never replace the stable installation for these tests. All tool invocations require
both parameters. Full captures and exact identities stay local outside Git.

Take one baseline per scenario: boot identity, owner/probe PID and kernel starttime,
heartbeat sequence, lease, vendor settings and native charging readback. Change
one variable at a time, only with supervision. Stop on unknown identity, unexpected
charging state, device instability or a tool outside its lease. Finish by stopping
the probe, verifying termination, cleaning its own files and confirming normal
charging. The runtime refuses Clean while its worker holds the exclusive lock. Save output
before Clean or a new Start. Clean retains the lock inode/private directory and
staged bootstrap artifacts. The first supervised Start must also validate private
directory access, the selected candidate CLASSPATH and exact worker identity; API
fixtures and green CI do not prove Android SELinux behavior. Do not switch or
replace staged APKs/launchers during a session.

## Scenarios

| Scenario | Procedure and acceptance | Evidence status |
|---|---|---|
| Individual Recents removal | Observe unchanged boot/PID/starttime and continuing heartbeats | Earlier bounded local observation passed; revised harness pending |
| Clear All | Start a 600-second probe, capture baseline, owner performs Clear All once, capture identity and heartbeat continuity | Pending |
| Standby cleaner | Record settings first; with the chosen cleaner configuration already approved, use a 600-second probe and observe one cleaner interval without changing other settings | Pending |
| Sleep without USB/ADB | Start a 1800-second heartbeat probe, disconnect USB, sleep for 10 minutes, reconnect before lease expiry; inspect retained identity, heartbeat gaps and stop event | Pending; gaps show scheduling pauses, not automatically death |
| Sentinel lease/stop | Use the fixed 30-second sentinel lease; test expiry and explicit Stop separately, verify expected marker and exit before Clean | Host-covered; revised harness physical confirmation pending |
| Exact owner identity | Compare boot/PID/starttime before and after a naturally restarted owner; PID reuse/mismatch is host-tested, not induced on the device | Partial |
| App update | Only after approval of an exact signed update and recovery plan, observe probe/owner transition and cleanup; do not uninstall or clear data | Deferred |
| Owner loss | Earlier harmless sentinel observed owner disappearance; do not repeat Force Stop or kill processes without separate supervised authorization | Partial; no charging restore proven |

## Interpretation and later production work

A continuing process with a large heartbeat gap proves survival with delayed
scheduling, not uninterrupted monitoring. USB-attached screen-off observation
does not establish USB-free deep sleep. Shell output, launch success and sentinel
markers do not establish actual charging restoration.

PR #11 corrected restoration before ARMED on unplug. Abrupt death while ACTIVE
is a separate scenario. Issue #2 records successful vendor protection workarounds;
do not reopen it merely because these new helper experiments remain pending.

The current sentinel has no renewal API or restore operation. A future restore-only
prototype requires a separate PR defining authenticated lease renewal, exact
ownership, bounded waiting and restore/readback failure behavior. Do not implement
that prototype or a full charging daemon in this research PR.
