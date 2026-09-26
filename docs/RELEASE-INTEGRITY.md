# Release integrity

## Current signed pre-release

- Package: `com.jesty.rpchargingseparation`
- Version code: `13`
- Version name: `1.3.0-dev`
- Debuggable: `false`
- APK: `Jesty-RP-Charging-Separation-1.3.0-dev.apk`
- Signed APK SHA-256:
  `51CA5C93F90ECA4AA4EEDA2A1F675AA9072AAEF98FA157DE1B503B426D5AF6BE`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The APK was rebuilt from the public source and signed outside the repository.
It remains a pre-release because the renamed package and final artwork have not
yet completed the real-device checklist, and RP5 end-to-end power-flow
validation is still pending.

## Final release procedure

1. Run a clean build from the reviewed Git commit.
2. Sign outside the repository with the selected Jesty release certificate.
3. Run `apksigner verify --verbose --print-certs`.
4. Record the exact commit, APK SHA-256, signer certificate SHA-256, package,
   version code, and version name.
5. Complete real-device validation with that exact signed APK.
6. Attach the APK to a GitHub Release; do not commit it to Git history.
