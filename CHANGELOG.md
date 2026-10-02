# Changelog

## 1.5.1

- Combined the left-side controls into one panel and moved reboot persistence
  to the end under the label **Maintain bypass charging after reboot**.
- Disabling Bypass charging now clears and disables the dependent mode and
  reboot options; turning it back on starts with Right away selected.
- Anchored the live dashboard at the bottom right on wide screens, placed the
  device and charging state under its heading, and removed the footer badge and
  diagnostic legend.

## 1.5.0

- Documented maintainer and Reddit community compatibility testing for Pocket
  5, Flip 2, Pocket Mini, and Pocket Mini V2, with device-validation scope noted
  separately.
- Redesigned the dashboard with a centered logo, live panel, clearer status
  wording, and numeric stop/charge-again controls.
- Stores an absolute charge-again level (70% by default) at least five
  percentage points below the stop level (80% by default).
- Migrates the resume margin saved by 1.5.0-rc1 to the equivalent absolute
  level without clearing the user's other settings.
- Increased versionCode to 19 so installations of 1.5.0-rc1 can update in
  place.

## 1.5.0-rc1 (pre-release)

- Added **Immediate** and **Automatic limit** separation modes. Automatic limit
  charges normally up to a configurable limit (80% by default), separates
  there, and resumes charging after the battery drops by a configurable margin
  (10 percentage points by default), e.g. separate at 80% and resume at 70%.
- Removed the "Support device testing or star the project." badge text.
- Rejects invalid battery-percentage readings before automatic mode can engage.

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
