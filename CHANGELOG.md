# Changelog

## 1.4.2

- Replaced the irregular launcher bolt with a simple centered yellow bolt on a
  solid purple circular background.
- Preserved the notification icon, dashboard artwork, charging controls,
  monitoring, safety fallback, and boot behavior unchanged.
- Bumped the stable update to versionCode 17 / versionName 1.4.2.

## 1.4.1

- Clarified that compatibility is capability-based rather than restricted to
  a Flip 2 or RP5 model-name allowlist.
- Added the Android device model to the live diagnostic line so Pocket Mini V2
  test results can be identified without publishing device serials.
- Made unsupported-device errors name the exact native charging-control node
  that is missing.
- Added the Retroid Pocket Mini V2 as a candidate device pending validation on
  the exact downloadable APK; charging control and safety fallback are
  unchanged.

## 1.4.0

- Added the final transparent Jesty RP Charging Separation lockup to the app
  and README without checkerboard reconstruction.
- Replaced both dashboard states with the maintainer-approved 1920x1080
  separation/normal-charging artwork.
- Corrected the header viewport to preserve the supplied 3:1 lockup ratio.
- Kept the yellow brand tints and replaced unsupported footer glyphs with
  reliable text-only links.
- Preserved the Binder control path, capability checks, monitoring, safety
  fallback, boot restoration, and hardware charging logic unchanged.
- Bumped the stable update to versionCode 15 / versionName 1.4.0.

## 1.3.0

- Prepared the first public Jesty source tree.
- Renamed the public application and package to remove private development
  identity from source and APK metadata.
- Preserved capability-based Retroid charging-control detection.
- Preserved readback validation, continuous monitoring, fail-closed restoration,
  and optional boot persistence.
- Added reproducible unsigned builds, public documentation, privacy checks, and
  explicit AI-assistance disclosure.
- Integrated maintainer-supplied charging-separated/battery-charging
  backgrounds and the approved horizontal project lockup.
- The dashboard artwork now follows confirmed charging-separation state.
- Added yellow brand tints to both dashboard toggles.
- Added the compact Support Jesty / Star on GitHub footer.
- Bumped the public update to versionCode 14 / versionName 1.3.0.
