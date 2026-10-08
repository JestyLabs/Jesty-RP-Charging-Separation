# Contributing

Keep changes narrow, reviewable, and tied to measured device behavior.

- Never commit APKs, signing keys, passwords, device serials, private paths, or
  unredacted logs.
- Preserve the fail-closed restoration path.
- Do not add a device to the confirmed list from model similarity alone.
- Include firmware, charger, and power-flow evidence for compatibility claims.
- Disclose material generative-AI assistance.
- Replace Jesty branding in redistributed forks unless permission was granted.

Before submitting a change, run a clean unsigned build and review the complete
diff for personal information.

## Research and attribution

Keep existing license, SPDX and provenance notices intact. If a change materially uses external code, research or prior art, include the upstream project and an exact commit or URL where practical. Record project-specific device findings and test provenance in [PROVENANCE.md](PROVENANCE.md).

## Security research publication

Keep detailed security findings and raw evidence local. Publish sanitized summaries
only, with synthetic device/session identifiers. See [publication policy](SECURITY-PUBLICATION.md).
