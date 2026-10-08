# Branch prune ledger — 2026-10-07

Purpose: reduce branch clutter without losing unique project information.

## Rule used

A branch is eligible for deletion only when:
1. its result is already represented on `main`; or
2. its exact work is retained by a merged/closed PR and the head SHA is pinned here.

Active PR branches are not pruned.

## KEEP

- `main`
- `fix/issue2-exit-diagnostics` — head `27b72ff0893ed395ab5475c4f07cc83ddd73f1be`; active PR #5
- `chore/pre-prune-archive` — temporary; delete only after this archive PR is merged

## SAFE TO DELETE AFTER THIS ARCHIVE IS MERGED

- `chore/actions-node24` — head `674d473a030ee29dfe7f40d8af3a61f45bdc093c`; PR #4 merged; branch is fully behind `main`
- `chore/repo-housekeeping` — head `da77431b00f1bdb39f2cbd26dc96041191349b50`; PR #7 merged
- `docs/provenance-record` — head `ed56579c4cc31bcf0cc917278186b751705f4ec3`; PR #6 merged
- `fix/issue2-reliability` — head `f4863c9db50bf090cd6b579c9ab652082036fe7b`; PR #3 merged; branch is fully behind `main`
- `fix/process-recovery-1.5.4` — head `aa733ec3b83c6d84a0f6b07ba9316793e7ed1c79`; PR #1 merged; branch is fully behind `main`

No orphaned unique branch was found in this repository at the time of the audit.

## Important

Deleting a branch is ref cleanup only. Do not delete Issue #2, PR #5, release notes, provenance records, or release artifacts as part of branch housekeeping.
