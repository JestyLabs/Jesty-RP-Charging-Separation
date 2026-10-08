# Flip 2 / Thor PServer research assessment

Status: research only; public summary, 2026-10-08.

The locally verified stock servers are close derivatives with a common Binder
core. They are not byte-identical. Detailed security/binary evidence, raw captures
and session identifiers remain local outside Git.

See the [consolidated public summary](https://github.com/JestyLabs/Jesty-Thor-Fix/blob/research/pserverbinder-lifecycle-readonly/docs/PSERVER-CROSS-DEVICE-EVIDENCE.md).

## Engineering implications

- Keep the current released transport convention.
- Treat transaction acceptance separately from successful hardware action/readback.
- Keep privileged commands/results fixed, small and explicitly validated.
- Capture boot/PID/starttime plus UID, cmdline, groups/cgroups and OOM metadata.
- Reuse authenticated owner-identity and bounded-lifetime principles from Thor.
- Do not infer a Retroid boot hook, CPU behavior or cleaner survival from Thor.
- Preserve typed adapters between policy/UI and the privileged transport.

The current clients resolve Binder per call. A cache is optional. Any future cache
must invalidate the exact stale instance and reacquire boundedly, without blindly
replaying a command whose execution is ambiguous. A process-wide lock orders only
this application's operations. No production transport change is implemented here.

## Next concrete test

One harmless leased sentinel through Retroid Clear All with separation OFF,
capturing exact identity and continuing heartbeat before/after. Normal close,
individual Recents removal, standby cleaner, USB-free sleep, Force Stop and package
replacement are separate physical gates. Use explicit stop and lease expiry.

Android remains the charging-policy owner. A future helper remains restore-only
and receives no charging authority before physical validation. The comparison
does not select the cause of the historical Thor incident or prove Flip 2 survival.
Existing local probe changes and physical notes remain preserved separately.

See [publication policy](../../SECURITY-PUBLICATION.md).
