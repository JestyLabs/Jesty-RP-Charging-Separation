# Changelog

## Unreleased

- Add Android's recent process exit reasons to COPY DIAGNOSTICS and persist
  service-start and task-removal events. This helps distinguish a crash, low
  memory kill, and user-requested stop when monitoring disappears.
- Add a read-only Retroid process-protection probe to COPY DIAGNOSTICS. It
  lists system settings through the existing PServer bridge, filters locally
  to whitelist/cleaner candidates, and never writes vendor settings.

## 1.5.9

- Fix issue #2 more fully: battery level and USB plug/unplug events now wake
  the service and re-check the stop level, so it no longer depends only on the
  two-second timer, which Android pauses in deep sleep. This covers turning
  bypass on before plugging in, then plugging in with the screen off.
- A temporary failure (charger not settled, validation mismatch, telemetry read
  error) now keeps normal charging and retries no sooner than 15 s, 30 s, 60 s,
  up to 5 min, when the service next runs. The status shows **TRYING AGAIN**.
- An unsupported device or failed restore keeps its notification visible after
  the service stops.
- When bypass is on, the service starts at boot or after an update even without
  USB, and waits for the cable.
- Warn when Android restricts the app in the background, and add **COPY
  DIAGNOSTICS** for bug reports: version, firmware, settings, readings, and the
  recent events saved on the device.
- Move the state machine into `BypassController`, which runs without Android.
  Add scenario tests for it, `scripts/test-all.ps1`, and a GitHub Actions
  workflow that also builds an unsigned APK.
- Accept standard Android SDK platform directories even when extension SDKs
  are installed beside them in CI.
- Remove the small LIVE label, make the charging state prominent in the wide
  dashboard card, rename the active state to RUNNING FROM CHARGER, and allow
  the screen to time out while the app is visible.

## 1.5.8

- Keep the CPU awake only while automatic mode charges toward the stop level,
  so the two-second monitor can apply native bypass when the screen sleeps.
- Release that wake lock at the stop level, on USB disconnect, on disable,
  and when the service ends.

## 1.5.7

- Simplify the LIVE card header: align the status with LIVE, place the device
  and charging details on full-width lines, and remove duplicate state text.
- Put GITHUB before SUPPORT beside the UPDATE button.
- Keep the in-app update prompt short and focused on the version and settings.

## 1.5.6

- Add an in-app update prompt and UPDATE button for newer stable GitHub releases.
- Download only the release APK after user confirmation. Check its size, GitHub
  SHA-256 digest, package, version code, and signing certificate before handing
  it to Android's installer for final confirmation.
- Resume a previously enabled bypass after the package is replaced when USB is
  connected, using the existing package-replaced receiver.

## 1.5.5

- Place the charging status inside the wide LIVE DASHBOARD card, beside its
  title and device line. Give that shared header more vertical room. Visually
  checked on a Retroid Pocket Flip 2.

## 1.5.4

- Promoted the process-recovery and dock-settling changes from 1.5.4-rc1 to
  the stable release, with `versionCode 24` for an in-place update.
- On a Flip 2, a controlled process crash restarted the foreground service and
  reattached to the native bypass; explicit OFF restored normal charging.
- The RP5 dock settling change still needs a device retest. The Flip 2 vendor
  launcher was observed invoking Android Force Stop from Recents, which prevents
  automatic service restart until the app is opened.

## 1.5.4-rc1 (device validation candidate)

- Reattach a restarted foreground service to Retroid's existing native bypass
  state and recover automatic charge-limit hysteresis after process death.
- Persist the user's switch choice before starting or stopping the service.
  On reopen, reconcile it with the native charge limit.
- Keep the sustained-discharge safety stop and show its reason in the app.
  Add lifecycle logs to distinguish that stop from Android process death and
  the vendor launcher's Force Stop behavior.
- Allow USB/dock power status to settle before applying and confirming native
  separation, avoiding an immediate false failure while a dock negotiates.

## 1.5.3

- Moved the current charging status from the left controls to a compact panel
  directly above the live dashboard on wide landscape screens. Narrow screens
  keep the status above the controls.
- Added maintainer-supplied v1.5.2 Flip 2 screenshots to the README, documenting
  the two mode views and normal charging toward an 80% stop level.

## 1.5.2

- Preserve the selected charging mode when Bypass charging is switched off and
  restore its radio selection when switched on again. The dependent controls
  stay disabled while bypass is off, and reboot persistence still turns off.
- Hide the scroll indicator without removing swipe scrolling.
- Widen the live dashboard on wide screens to close the gap between panels.
- Capitalize the Bypass charging and mode labels and simplify the Right away
  helper text.

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
