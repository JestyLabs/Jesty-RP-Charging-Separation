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

## Stable 1.4.0 release

- Package: `com.jesty.rpchargingseparation`
- Version code: `15`
- Version name: `1.4.0`
- Debuggable: `false`
- APK: `Jesty-RP-Charging-Separation-1.4.0.apk`
- Signed APK SHA-256:
  `D7D1B598CDB12E5CCDA3CE2E90A5E8C1CA5BFB9AFC578567DFEFA1FF3447BF58`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The signed 1.4.0 APK was installed in place on the physical Retroid Pocket
Flip 2 and its package identity was confirmed. The charging-control path is
unchanged from 1.3.0. The final rebuild differs from the exercised 1.4.0
candidate only in header/badge presentation and disclosure copy.

## Stable 1.4.1 Pocket Mini V2 compatibility candidate

- Package: `com.jesty.rpchargingseparation`
- Version code: `16`
- Version name: `1.4.1`
- Debuggable: `false`
- APK: `Jesty-RP-Charging-Separation-1.4.1.apk`
- Signed APK SHA-256:
  `B1F9D3D5461615CC90E70219C8355C1721B041588FE1D71D94BAD97B18ADACD0`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The clean build and signature verification passed. The compatibility gate is
capability-based and the safety/control path is unchanged from 1.4.0. This
exact APK has not yet been exercised on a physical Pocket Mini V2, so Mini V2
support remains a candidate until an owner confirms the native nodes,
`Not charging` state, USB input, and safe restoration to normal charging.

## Stable 1.4.2 launcher-icon refresh

- Package: `com.jesty.rpchargingseparation`
- Version code: `17`
- Version name: `1.4.2`
- Debuggable: `false`
- APK: `Jesty-RP-Charging-Separation-1.4.2.apk`
- Signed APK SHA-256:
  `64A48CF53122065B8B4EA0D686EEB0B7AC91938C517C3B4D1EAC82A381660C08`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The release changes only the launcher vector and version metadata. The
notification icon and all charging, monitoring, safety, and boot code are
unchanged from 1.4.1. The exact signed APK passed static artifact validation but
was not installed on the physical Flip 2 during this release round.

## 1.5.0-rc1 automatic-limit pre-release

- Package: `com.jesty.rpchargingseparation`
- Version code: `18`
- Version name: `1.5.0-rc1`
- APK: `Jesty-RP-Charging-Separation-1.5.0-rc1.apk`
- Signed APK SHA-256:
  `BF2AC549B28C9AD850D0C6F6F4459EE2CA69E4EFFE14B547DD76E412FE1C7E24`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The signed APK passed build, package, alignment, and signature checks. The
automatic-limit cycle remains unverified on a physical Retroid device; this
artifact is a pre-release and must not be promoted to stable until that test
uses this exact APK.
