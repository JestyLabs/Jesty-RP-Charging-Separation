<!-- SPDX-FileCopyrightText: 2026 Jesty Labs contributors -->
<!-- SPDX-License-Identifier: GPL-3.0-only -->

# Research workflow

This project keeps reverse-engineering and device-behavior claims traceable without turning the repository into an attribution dispute.

## Before publishing a substantial new finding

For non-trivial reverse engineering, vendor-behavior discoveries, or implementation approaches that may be reused across projects:

1. Record the finding in `PROVENANCE.md` before or in the same PR that makes it public.
2. Give the record a stable, dated research ID.
3. Classify the claim as **PROVEN**, **OBSERVED**, **HYPOTHESIS**, or **UNTESTED**.
4. Link the exact commit/PR/issue, device build, test artifact hash, or other evidence that supports it.
5. Record material external prior art with an upstream project and exact commit/URL where practical.
6. Keep implementation provenance tied to the exact physically tested candidate when later refactors or consolidation change the final code shape.

## Source-of-truth order

When continuing or reviewing work, use this order:

1. current repository code and open PRs;
2. `PROVENANCE.md` research records;
3. project architecture / validation documents;
4. raw logs or local handoff notes when a claim cannot be reconstructed from the repository.

Do not treat an old chat summary or design note as stronger evidence than current code or a recorded physical test.

## Global or vendor-owned state

For settings, files, hooks, whitelists, or other state not exclusively owned by this app:

- read before writing;
- preserve unknown and pre-existing values;
- mutate only the minimum required value;
- verify exact readback;
- record whether the app created/added the state when that matters for safe cleanup;
- never infer ownership from a matching filename, package fragment, PID, port, or setting substring alone.

## Attribution and comparisons

Generic Android/vendor mechanisms and public prior art are not claimed as project inventions.

If another project appears to implement a similar idea, compare objective evidence first: public timestamps, commits, code structure, unusual implementation details, identical mistakes, copied text, and license/notice handling. Similar functionality alone is not evidence of copying.

Do not add fake bugs, misleading code paths, poisoned examples, or traps. Provenance should come from legitimate technical records that remain useful even if no dispute ever occurs.

## Release discipline

A research record is not a release claim. Hardware-sensitive behavior still requires the project's normal review, CI, and physical validation before release.
