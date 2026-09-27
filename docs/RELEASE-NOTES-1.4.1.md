# Jesty RP Charging Separation 1.4.1

This stable compatibility release makes the existing capability-based device
support explicit for Retroid Pocket Mini V2 testing.

## What changed

- The app remains free of a Flip 2/RP5 model-name allowlist: compatible Retroid
  devices are accepted when the required native charging-control nodes exist.
- The live diagnostic line now includes Android's device model, making a Mini
  V2 result identifiable without collecting a serial number.
- An unsupported-device error now names the exact required control node that
  is missing.

## Safety is unchanged

The app still writes only Retroid's native `charge_control_limit`, reads the
value back, requires Android to report `Not charging`, monitors USB and battery
flow, and restores normal charging on validation or telemetry failure.

## Validation status

- Retroid Pocket Flip 2 remains the maintainer-validated device.
- Retroid Pocket 5 remains user-reported as working.
- Retroid Pocket Mini V2 is a capability-compatible candidate awaiting a test
  of this exact APK on the physical device.

Mini V2 testers should confirm that the app reports `CHARGING SEPARATED`,
`Not charging`, and matching native limit values while USB input remains
present. Do not share device serials or raw logs publicly.

## Artifact

- Version: `1.4.1` (`versionCode 16`)
- APK: `Jesty-RP-Charging-Separation-1.4.1.apk`
- SHA-256: `B1F9D3D5461615CC90E70219C8355C1721B041588FE1D71D94BAD97B18ADACD0`
- Signing certificate SHA-256:
  `727D4850779BED1E51018108E13BC399D4DA38CFC68F4F7504120AD5E2DAD6FC`
