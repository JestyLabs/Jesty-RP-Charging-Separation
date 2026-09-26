# Release integrity

## Current 1.3.0 signed candidate

- Package: `com.jesty.rpchargingseparation`
- Version code: `14`
- Version name: `1.3.0`
- Debuggable: `false`
- APK: `Jesty-RP-Charging-Separation-1.3.0.apk`
- Signed APK SHA-256:
  `37A8657E935D903DBF185CFE1572B5A9257249B93BE01B3AA5880918D823BA46`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The APK is rebuilt from public source and signed outside the repository. At
initial pre-release publication, this exact 1.3.0 artifact has not yet completed
the Pocket Flip 2 checklist. Results are added after post-release validation.
The RP5 has been reported working with the app, but no RP5 telemetry capture is
presented as maintainer evidence until a sanitized repeatable capture is
available.

## Final release procedure

1. Run a clean build from the reviewed Git commit.
2. Sign outside the repository with the selected Jesty release certificate.
3. Run `apksigner verify --verbose --print-certs`.
4. Record the exact commit, APK SHA-256, signer certificate SHA-256, package,
   version code, and version name.
5. Complete real-device validation with that exact signed APK.
6. Attach the APK to a GitHub Release; do not commit it to Git history.
