<!-- SPDX-FileCopyrightText: 2026 Jesty Labs contributors -->
<!-- SPDX-License-Identifier: GPL-3.0-only -->

# Research provenance

This file keeps technical provenance for Jesty RP Charging Separation explicit with links to our implementation and validation evidence.

See [docs/RESEARCH-WORKFLOW.md](docs/RESEARCH-WORKFLOW.md) for the standing workflow used before publishing substantial new reverse-engineering or cross-project findings.

## Project evidence and publication

Keep research records tied to our exact commits, tests and observed outcomes. Public material focuses on our architecture, implementation and results. Detailed analysis and device/session captures remain local. Preserve legally required licenses and attribution.

## Platform boundary

The app uses the vendor PServerBinder bridge to access privileged charging controls. Bridge availability and process-protection policy remain firmware-specific; no generic ownership claim or cross-device guarantee is implied.

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

PR #5 records local whitelist backing-store discovery and read-only comparison. Reporter tests in issue #2 showed each tested vendor protection could preserve the threshold transition; whitelist also preserved the notification. Those results do not demonstrate privileged helper survival.

### JRPCS-RR-20261007-PSERVER-PROCESS-RESILIENCE

PR #9 investigates bounded helper survival without charging authority. The Android controller remains the charging-policy owner.

- HOST-TESTED: exact PID/starttime parsing, restore-only policy decisions, bounded commands, sentinel generation and retry policy.
- OBSERVED ON LOCAL DEVICE: individual Recents removal and about two minutes screen-off with USB/ADB attached preserved the probe; the harmless sentinel detected owner loss. No charging restoration was performed.
- HISTORICAL: the ACTIVE -> unplug nonzero ARMED window was fixed by PR #11. Updated tests assert restore before ARMED; abrupt ACTIVE owner death remains distinct.
- PENDING: Clear All, standby cleaner, USB-free deep sleep scheduling, controlled update, exact identity mismatch and cleanup coverage for the revised harness.

The released transport convention remains unchanged. Detailed analysis and raw evidence stay local. See docs/research/RETROID-RESEARCH-VALIDATION-MATRIX.md for the physical research gates.

## Evidence discipline

Technical claims should be labelled internally as:

- **PROVEN**
- **OBSERVED**
- **HYPOTHESIS**
- **UNTESTED**

For privileged mutations, preserve existing vendor-global state, change only the minimum required value, and verify exact readback.

## Licensing metadata

Maintained source, tests and scripts are covered by the repository's GPL-3.0 terms. Central SPDX annotations are in [REUSE.toml](REUSE.toml). Branding and artwork remain separate from the code license.
