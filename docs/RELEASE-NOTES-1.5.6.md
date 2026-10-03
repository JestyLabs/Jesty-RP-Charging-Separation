# Jesty RP Charging Separation 1.5.6

This release adds in-app updates. When a newer stable release is available,
the app shows an UPDATE button beside SUPPORT and GITHUB and offers to install
it. The app checks GitHub when opened, at most once an hour. It ignores drafts
and pre-releases.

After confirmation, the app downloads the release APK and checks its size,
SHA-256 digest, package name, higher version code, and signing certificate.
Android asks for installation permission and final confirmation. Saved bypass
settings are preserved during an in-place update; the existing package-replaced
receiver can resume an enabled bypass when USB is connected.

## Verification

- Java version and charge-limit policy tests passed.
- Android build, ZIP alignment, package/version inspection, and APK signature
  verification passed.
- The exact signed APK is version `1.5.6` (`versionCode 27`) and uses the
  existing signing certificate.
- The first end-to-end in-app update on a Retroid device is pending after the
  release becomes available to the installed v1.5.5 app.

## APK

- File: `Jesty-RP-Charging-Separation-1.5.6.apk`
- SHA-256: `8CA5D7B3BBF41A337BC5F27CE38956C9B3857F5759F5D2007268B6F5B8AE6A7B`
