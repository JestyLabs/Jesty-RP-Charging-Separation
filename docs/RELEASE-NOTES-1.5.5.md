# Jesty RP Charging Separation 1.5.5

The charging status now appears inside the LIVE DASHBOARD card on wide
landscape screens, beside its title and device line. The shared header has
more vertical space for longer status messages. On narrower screens, status
remains above the controls.

Charging behavior and saved settings are unchanged from 1.5.4.

## Verification

- The layout was visually checked on a Retroid Pocket Flip 2 with an in-place
  testing build. Its long `RUNNING FROM USB` status and detail text fit in the
  shared card without clipping.
- The Java charge-limit policy test and Android build passed. The APK was
  signed with the existing certificate and inspected for version/alignment.
- The exact stable APK was installed on the Flip 2 for a final smoke check.

The narrow-screen layout and the reported RP5 dock transition have not been
retested for this UI release.
