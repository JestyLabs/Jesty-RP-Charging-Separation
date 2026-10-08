# PServerBinder wire-contract evidence

Status: **research note**. No production bridge changes are authorized by this document.

## Cross-device research update (2026-10-08)

The [public assessment](PSERVER-CROSS-DEVICE-ASSESSMENT.md) confirms compatibility
of the current transport for the acquired pair. Keep the released convention.
Detailed server/security analysis remains local. The external survey below records
the evidence available before that local comparison and does not justify changing
the payload or adding automatic recovery. Acceptance and command success remain
separate; commands/results must remain fixed and bounded.

## Current Jesty contract

The Charging Separation app currently calls `ServiceManager.getService("PServerBinder")`
and sends transaction code `0` with:

```text
String[]{ command, "0" }
```

The reply is decoded with `Parcel.createByteArray()`.

This exact contract is already used by the released charging-control path and is therefore
the only contract we currently treat as **PROVEN FOR THIS APP / HARDWARE FAMILY**.

The Thor project independently uses the same second element `"0"`.

## What public implementations show

Public AYN/Retroid-related projects are split:

### Uses second element `"0"`

Examples include:

- Jesty Thor Fix
- Jesty RP Charging Separation
- ClusterTune
- Thor Wayfinder
- other Thor utilities derived from decompiled stock-settings behavior

Thor Wayfinder documents its contract as reverse-engineered from the stock
`com.odin.settings` client and reports `[command, "0"]`.

### Uses second element `"1"`

Examples include:

- OdinTools
- GameNative
- Argosy Launcher
- several other PServer helpers

Some projects label the second field "wait-for-result". Another public implementation
labels `"1"` a root/run flag.

Those labels conflict with each other and neither is accepted here as authoritative
server-side semantics.

## What we did NOT find

A public, authoritative source for the actual vendor `pservice` server implementation
that clearly parses the second String-array element.

Therefore the semantic meaning of `"0"` vs `"1"` remains **UNKNOWN**.

## Decision

Do not change `RootBridge` from `"0"` to `"1"` based on popularity or copied client
code.

A future change would require one of:

1. decompilation/disassembly of the exact Retroid `/system/bin/pservice` or its binder
   implementation showing how element 1 is parsed; or
2. controlled physical tests comparing `"0"` and `"1"` on harmless commands and
   process-launch behavior.

Until then:

```text
current "0" = proven working contract
"1" = alternate public-client convention, semantics unproven
```

## Additional PServer quirks relevant to this project

External reverse engineering reports several vendor-specific failure modes:

- long inline commands may be truncated or silently ignored;
- multi-line stdout may be truncated;
- overlapping Binder transactions may produce empty replies;
- shell backgrounding/redirection inside the Binder command can behave differently from
  a normal interactive shell;
- one Thor investigation reports `setsid app_process` as unreliable;
- script-file indirection (`sh /path/to/script`) is reported as more robust.

These are **external implementation observations**, not yet proven on Flip 2.

The research survival probe was changed accordingly:

- ADB stages a launcher script under `/data/local/tmp`.
- PServer receives only `sh <fixed-path>`.
- The script does the bounded background launch.
- `setsid` is deliberately not used.

This makes the physical test primarily about process lifetime rather than PServer parser
quirks.

## Concurrency finding in current app

`BypassService` serializes its own controller work through a single worker thread, so
its charging writes are naturally ordered.

However, `RootBridge.exec()` itself is not globally serialized and does not cache or
reacquire a dead Binder. A diagnostics/UI path can theoretically transact while the
service is also using PServer.

No production bug is currently attributed to this.

Long-term bridge refactoring should consider:

1. one process-wide transaction lock;
2. cached Binder only while `isBinderAlive()`;
3. reacquisition after dead Binder;
4. explicit distinction between transport acceptance and command/result validation;
5. bounded command length;
6. no generic arbitrary-shell API exposed outside a narrow privileged-adapter layer.

Do not perform that refactor as part of the current read-only whitelist or survival
research.
