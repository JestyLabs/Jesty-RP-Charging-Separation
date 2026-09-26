# Public release checklist

## Privacy and identity

- [x] Public staging tree is separate from the private build workspace.
- [x] Public Java package uses the Jesty identity and contains no personal name.
- [x] No signing keys, passwords, tokens, device serials, local home paths, or
  private logs are included.
- [x] Configure the repository-local Git author as `SirJesty` with a non-personal
  `users.noreply.github.com` address.
- [x] Configure `origin` for `SirJesty/Jesty-RP-Charging-Separation` without
  pushing.
- [ ] Confirm the configured no-reply address exactly matches the value shown by
  the new GitHub account before the first commit.
- [ ] Review the complete staged diff before the first commit.

## Artwork

- [x] Unapproved legacy artwork is excluded.
- [x] The maintainer supplied final charging-separated and battery-charging
  backgrounds.
- [x] The exact transparent Jesty wordmark is used in the top-left header.
- [x] Runtime state selects the matching background.
- [x] Final artwork terms are recorded in `ASSETS-LICENSE.md`.
- [ ] Verify final screenshots on real hardware.

## Build and hardware validation

- [ ] Clean unsigned build succeeds from this repository.
- [ ] Sign with the release certificate outside the repository.
- [ ] Verify package, version, certificate, and APK SHA-256.
- [ ] Validate enable, sustained monitoring, safe fallback, and disable on a
  supported Retroid device.
- [ ] Validate unplug/replug, app close, recents removal, normal reboot, and
  Android Settings -> Force stop behavior.
- [ ] Validate actual USB/device/battery power flow on RP5 before claiming RP5
  support.

## Publication

- [ ] Create the repository under `SirJesty` only after final review.
- [ ] Push the reviewed initial commit.
- [ ] Publish the APK through a GitHub Release, not in Git history.

Do not push or publish while any artwork, privacy, or hardware blocker remains.
