# Release integrity

## Current development build

- Package: `com.jesty.rpchargingseparation`
- Version code: `13`
- Version name: `1.3.0-dev`
- Debuggable: `false`
- Unsigned APK SHA-256 after final artwork integration:
  `F4C98D4B5E78F9F9DB2327C0280CF36A7C5DF312ED962308492034A126D0776F`

The development hash is recorded to make the current staging state
reproducible. It will change again when the final release version is selected
or the APK is signed.

## Final release procedure

1. Run a clean build from the reviewed Git commit.
2. Sign outside the repository with the selected Jesty release certificate.
3. Run `apksigner verify --verbose --print-certs`.
4. Record the exact commit, APK SHA-256, signer certificate SHA-256, package,
   version code, and version name.
5. Complete real-device validation with that exact signed APK.
6. Attach the APK to a GitHub Release; do not commit it to Git history.
