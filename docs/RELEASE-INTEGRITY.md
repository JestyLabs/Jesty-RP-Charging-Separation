# Release integrity

## 1.5.6 in-app updates

- Package: `com.jesty.rpchargingseparation`
- Version code: `27`
- Version name: `1.5.6`
- APK: `Jesty-RP-Charging-Separation-1.5.6.apk`
- Signed APK size: `6,067,119` bytes
- Signed APK SHA-256:
  `8CA5D7B3BBF41A337BC5F27CE38956C9B3857F5759F5D2007268B6F5B8AE6A7B`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The Java version and charge-limit policy tests, Android build, ZIP alignment,
package/version inspection, and APK signature verification passed. The
end-to-end in-app update is pending release availability on the Flip 2.

## 1.5.5 shared dashboard/status card

- Package: `com.jesty.rpchargingseparation`
- Version code: `26`
- Version name: `1.5.5`
- APK: `Jesty-RP-Charging-Separation-1.5.5.apk`
- Signed APK size: `6,058,927` bytes
- Signed APK SHA-256:
  `E44CEADB5D72EA4A139943B948A3FFA075B258A817023198654A3FDCC3DA2623`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The Java policy test, Android build, ZIP alignment and APK signature passed.
The exact stable APK installed in place on a Flip 2; the shared card was
visually checked while native separation remained active (`10/10`,
`Not charging`).

## 1.5.4 process recovery

- Package: `com.jesty.rpchargingseparation`
- Version code: `24`
- Version name: `1.5.4`
- APK: `Jesty-RP-Charging-Separation-1.5.4.apk`
- Signed APK size: `6,058,927` bytes
- Signed APK SHA-256:
  `00006BEB9C938CE3AADB77BCEBC1C15B3C6B977DC5F9F974E668F11A258B8F45`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The charge-limit policy test, Android build and signature verification passed.
The 1.5.4-rc1 behavior was tested on a Flip 2; this exact stable APK was not
installed before the device disconnected from ADB.

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

## 1.5.0 UI and absolute charge-again level

- Package: `com.jesty.rpchargingseparation`
- Version code: `19`
- Version name: `1.5.0`
- APK: `Jesty-RP-Charging-Separation-1.5.0.apk`
- Signed APK SHA-256:
  `11A12BC9F6EE3F10C8B72B7FE3E7DC1BD4FBFABF304E86177E19EC2E543EA299`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The exact APK passed static build and signature checks. No Retroid was connected
for the 1.5.0 build, so the new layout and charge/resume cycle remain pending
physical validation.

## 1.5.1 compact controls and live dashboard

- Package: `com.jesty.rpchargingseparation`
- Version code: `20`
- Version name: `1.5.1`
- APK: `Jesty-RP-Charging-Separation-1.5.1.apk`
- Signed APK size: `6,058,927` bytes
- Signed APK SHA-256:
  `ED467719B7810EB399D6952A8E8F128C696395965D8CB2228233D2FE614A1021`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The Java policy test, Android build, ZIP alignment, package/version inspection,
and APK signature verification passed. No Retroid was connected by ADB for this
build, so the exact 1.5.1 layout and behavior have not been checked on device.

## 1.5.2 mode selection and layout polish

- Package: `com.jesty.rpchargingseparation`
- Version code: `21`
- Version name: `1.5.2`
- APK: `Jesty-RP-Charging-Separation-1.5.2.apk`
- Signed APK size: `6,058,927` bytes
- Signed APK SHA-256:
  `A9DAD181872481E862F8BB5D324C2C9E34EE0A9099CBEB5481B22E8B4F0D5A09`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The Java policy test, Android build, ZIP alignment, package/version inspection,
and APK signature verification passed. No Retroid was connected by ADB for this
build; the exact 1.5.2 mode-toggle sequence and screen layout still need a
physical device check.

## 1.5.3 charging-status placement

- Package: `com.jesty.rpchargingseparation`
- Version code: `22`
- Version name: `1.5.3`
- APK: `Jesty-RP-Charging-Separation-1.5.3.apk`
- Signed APK size: `6,058,927` bytes
- Signed APK SHA-256:
  `6C3EAC817095977C14801975815525989FC47A7C6DDEA833FF10783B18D27D37`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
- Verified signing scheme: APK Signature Scheme v3.

The Java policy test, Android build, ZIP alignment, package/version inspection,
and APK signature verification passed. The status placement has not yet been
visually checked on a physical Retroid.
