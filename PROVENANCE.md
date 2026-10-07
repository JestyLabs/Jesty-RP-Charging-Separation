<!-- SPDX-FileCopyrightText: 2026 Jesty Labs contributors -->
<!-- SPDX-License-Identifier: GPL-3.0-only -->

# Research provenance

This file keeps technical provenance for Jesty RP Charging Separation explicit without claiming ownership of generic Android, Retroid or AYN platform mechanisms.

See [docs/RESEARCH-WORKFLOW.md](docs/RESEARCH-WORKFLOW.md) for the standing workflow used before publishing substantial new reverse-engineering or cross-project findings.

## Scope and attribution rule

When project work materially depends on external code, research or prior art, record the upstream source and an exact URL or commit where practical. When a claim comes from this project, preserve the commit, issue/PR, device build and test evidence that established it.

Downstream use remains governed by GPL-3.0. The project asks that non-trivial research and implementation provenance be preserved when reused.

## Public prior art and platform mechanisms

The vendor `PServerBinder` bridge predates this project. Jesty RP Charging Separation has used it since the initial public release to access Retroid's privileged charging controls.

OdinTools is an important public reference for adjacent vendor behavior:

- repository: https://github.com/langerhans/OdinTools
- reference commit: `0eaf49392e263bf1de4d6c7eb37aeb03dadb0ccc`
- `ShellExecutor.kt` uses `PServerBinder`
- `SettingsRepo.kt` manages an AYN vendor whitelist through the system setting `app_whiteList`

That is prior art for the vendor bridge and AYN whitelist behavior. It is **not** proof that Retroid's current Whitelist Application UI uses the same setting.

## Project research records

### JRPCS-RR-20260926-INITIAL

Initial public release: `3a2b006943d558ab898ed087f88a082f0c452f56`

`RootBridge.java` already used `PServerBinder` in this public release.

### JRPCS-RR-20261003-PROCESS-RECOVERY

Commit: `aa733ec3b83c6d84a0f6b07ba9316793e7ed1c79`

This work added recovery behavior for ordinary process death while native charging separation may remain active.

Follow-up commits:
- `0b8159155df79ecaee39008d49e00fe48c7cd22c` — keep automatic charge monitoring awake until the configured threshold
- `033c72c7901ccf0db347bef8aae549006a82fe4b` — event-driven charger/battery handling and retry behavior

### JRPCS-RR-20261006-FLIP2-311-PROCESS-PROTECTION

Primary evidence: [Issue #2](https://github.com/JestyLabs/Jesty-RP-Charging-Separation/issues/2)

Reporter device: Retroid Pocket Flip 2 12 GB

Firmware: `RPFlip2_HV1.0.0.311_20260725_132332_user`

Observed failure without vendor process protection:
- charger detection occurred
- charge-to-limit wake lock was acquired
- the service/process later disappeared during standby
- reopening the app produced a fresh ENABLE start

Independent reporter tests then showed:
- Retroid **Clean process when standby → Ignored packages** alone allowed the 80% transition to complete
- Retroid **Whitelist Application** alone also allowed the 80% transition to complete
- Whitelist Application also fixed the observed disappearing-notification behavior

The reporter deserves credit for the independent A/B testing that isolated those device settings.

Diagnostic follow-up: [PR #5](https://github.com/JestyLabs/Jesty-RP-Charging-Separation/pull/5)

### JRPCS-RR-20261007-RETROID-WHITELIST

Current research question:

Can the app read and, later, safely configure Retroid's own process whitelist through the same `PServerBinder` transport it already uses?

Current status:
- **PROVEN:** the `.311` failure is avoided when either tested Retroid process protection is enabled.
- **PROVEN:** OdinTools uses `app_whiteList` on AYN through the same vendor bridge family.
- **UNTESTED:** Retroid Whitelist Application uses that exact key/format.
- **PLAN:** read-only discovery first; no release or charging-behavior change until the Retroid backend is proven.

## Evidence discipline

Technical claims should be labelled internally as:

- **PROVEN**
- **OBSERVED**
- **HYPOTHESIS**
- **UNTESTED**

For privileged mutations, preserve existing vendor-global state, change only the minimum required value, and verify exact readback.

## Licensing metadata

Maintained source, tests and scripts are covered by the repository's GPL-3.0 terms. Central SPDX annotations are in [REUSE.toml](REUSE.toml). Branding and artwork remain separate from the code license.
