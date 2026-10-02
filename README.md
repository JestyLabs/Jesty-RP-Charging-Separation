<p align="center">
  <img src="assets/branding/jesty_rp_header_lockup.png" alt="Jesty RP Charging Separation" width="760">
</p>

<p align="center">
  <strong>Play while plugged in without continuously charging the battery.</strong><br>
  Uses Retroid's own charging controls, with live battery/USB telemetry and automatic safety fallback.
</p>

<p align="center">
  <strong>No Magisk. No terminal. No need to keep the app open.</strong><br>
  Install it, enable Bypass charging, and forget about it.
</p>

<p align="center">
  <a href="https://github.com/JestyLabs/Jesty-RP-Charging-Separation/releases/tag/v1.5.0"><strong>Download APK</strong></a>
  · <a href="#normal-charging-vs-charging-separation">How it works</a>
  · <a href="docs/BENCHMARKS.md">Measurements</a>
  · <a href="https://www.buymeacoffee.com/jesty">☕ Support development</a>
</p>

<p align="center">
  <img alt="Retroid Pocket Flip 2" src="https://img.shields.io/badge/compatible-Flip%202-7C3AED?style=for-the-badge">
  <img alt="Retroid Pocket 5" src="https://img.shields.io/badge/compatible-RP5-7C3AED?style=for-the-badge">
  <img alt="Retroid Pocket Mini" src="https://img.shields.io/badge/compatible-Mini-7C3AED?style=for-the-badge">
  <img alt="Retroid Pocket Mini V2" src="https://img.shields.io/badge/compatible-Mini%20V2-7C3AED?style=for-the-badge">
  <img alt="Android 13" src="https://img.shields.io/badge/Android-13-3DDC84?style=for-the-badge&amp;logo=android&amp;logoColor=white">
  <img alt="No Magisk or rooting" src="https://img.shields.io/badge/setup-no%20Magisk%20%2F%20rooting-16A34A?style=for-the-badge">
  <img alt="GPL 3" src="https://img.shields.io/badge/code-GPL--3.0-8B5CF6?style=for-the-badge">
</p>

## What the app does

Normally, USB powers the handheld and charges its battery at the same time.
Charging Separation uses Retroid's built-in privileged controls to stop active
battery charging while USB remains connected and continues supplying the
running device.

- No user-managed root, Magisk, Termux, or ADB setup.
- Live battery current, USB input, direct-to-device estimate, and temperature.
- **Right away** or **At a battery level**: stop active charging immediately,
  or charge to a chosen level (80% by default) and charge again at a second
  chosen level (70% by default). The charge-again level stays at least five
  percentage points below the stop level.
- Verifies that separation actually activated before reporting success.
- Restores normal charging if validation or safety telemetry fails.
- Optional restore after a normal reboot.
- Does not change CPU governors, CPU frequencies, or unrelated Android settings.

The app uses Retroid's own `PServerBinder` bridge. It does not physically
disconnect the battery, so small positive or negative currents can still appear
depending on load, charger, firmware, temperature, and battery state.

### Set a battery threshold

Enable **Bypass charging** and choose **At a battery level** under **When to stop
charging**. Set **Stop charging at** to the level where the app should switch to
USB power (80% by default). Set **Charge again at** to the lower level where
normal charging should resume (70% by default). You can type percentages or use
the −/+ buttons in five-point steps. The app adjusts invalid values and keeps
the two levels at least five percentage points apart. Choose **Right away** to
start separation as soon as it is available instead. Existing 1.5.0-rc1
settings are migrated automatically.

## Normal charging vs Charging Separation

| | Normal charging | Charging Separation |
| --- | --- | --- |
| USB connected | Yes | **Yes** |
| Handheld remains powered | Yes | **Yes** |
| Battery actively charging | Yes | **No** |
| Android battery state | `Charging` | **`Not charging`** |
| Live safety monitoring | — | **Yes** |
| App must stay open | — | **No** |
| Root/Magisk setup | — | **No** |

## Measured on Retroid Pocket Flip 2

A controlled comparison captured 30 one-second samples per state with the same
USB cable, screen state, workload, and ambient conditions:

| Metric | Normal charging | Charging separated |
| --- | ---: | ---: |
| Android state | `Charging` | **`Not charging`** |
| Native limit | `0/10` | **`10/10`** |
| Average battery current | +0.239 A | **-0.011 A** |
| Charge-counter change | +2,449 uAh | **-109 uAh** |
| Average USB input | 2.286 W | **1.379 W** |
| Direct-to-device proxy | 1.368 W | 1.422 W |
| Battery temperature | 29.7 C | 29.7 C |

The important behavior is consistent across the capture: Android changed to
`Not charging`, battery flow fell to approximately neutral, and USB remained
connected and continued supplying the handheld.

> [!NOTE]
> This is a short functional measurement, not a battery-health or longevity
> study. See the [method and sanitized samples](docs/BENCHMARKS.md).

## Compatibility

| Device | Evidence |
| --- | --- |
| **Retroid Pocket Flip 2** | Compatible; charging behavior measured on earlier builds and supported by community testing |
| **Retroid Pocket 5** | Compatible according to maintainer and community testing |
| **Retroid Pocket Mini** | Compatible according to community testing |
| **Retroid Pocket Mini V2** | Compatible according to community testing |
| Other Retroid models | Not validated — treat as unsupported until tested |
| Unrelated Android devices | Unsupported |

Compatibility is based on the required Retroid control nodes and privileged
bridge, not a model-name allowlist. Before enabling separation, the app checks
those capabilities and validates the resulting hardware state. A similar
chipset or device name is not treated as proof of successful operation.

The maintainer reports full charging-separation compatibility, tested with help
from owners in the
[Retroid community thread](https://www.reddit.com/r/retroid/comments/1wrl00f/noroot_charging_separation_app_for_rp5_flip_2/)
for the four models above. These are compatibility reports, not equivalent
per-device benchmark captures. The exact v1.5.0 APK, its redesigned screen, and
the complete automatic charge/resume cycle have not yet been independently
documented on every model; see [live results](docs/LIVE-RESULTS.md).

## Safety behavior

Before and while separation is active, the controller:

- verifies the charging-control capability and written limit;
- confirms Android reports `Not charging`;
- monitors USB input, battery current, charge counter, and temperature;
- restores normal charging if validation or telemetry fails;
- disables separation after repeated evidence that the battery may be powering
  the handheld instead of USB.

## Install and forget

1. Download `Jesty-RP-Charging-Separation-1.5.0.apk` from the
   [latest stable release](https://github.com/JestyLabs/Jesty-RP-Charging-Separation/releases/tag/v1.5.0).
2. Install and open **Jesty RP Charging Separation**.
3. Connect a suitable USB charger.
4. Enable **Bypass charging** and confirm `Not charging` in the live diagnostic.
5. Enable **Turn on again after restart** only if you want that behavior.

After that, the dashboard can be closed and the app can be individually swiped
away from Recents while the foreground controller continues monitoring the
state.

**Force stop is different.** Android Settings → Force stop blocks the app and
its boot receiver until it is opened again. On the tested Flip 2 firmware, the
vendor launcher's **Clear all** action also behaved like a force-stop; an
individual swipe of the app did not. Reopen the app after either force-stop
path.

The service uses lightweight periodic telemetry and no busy loop. Turning the
toggle off, or a safety failure, restores normal charging.

<details>
<summary><strong>Technical implementation</strong></summary>

The app reaches Retroid's hidden `PServerBinder` through Android's service
manager. The vendor transaction accepts a two-element String array and runs a
narrowly scoped command through Retroid's privileged `pservice` path.

See [Architecture](docs/ARCHITECTURE.md) for the state machine and failure
behavior.

</details>

<details>
<summary><strong>Build from source</strong></summary>

Requirements: PowerShell 5.1+, Android SDK platform 28+, Android Build Tools
34+, and JDK 17.

```powershell
.\build.ps1
```

Signing is opt-in and reads the password interactively. The repository contains
no signing key or password.

</details>

## Support and documentation

Jesty RP Charging Separation is free and open source. No feature is locked
behind donations.

- ⭐ Star the repository so other Retroid owners can find it.
- 🧪 Share carefully redacted results from another firmware or device.
- 🐛 Report reproducible safety or compatibility issues.
- ☕ [Buy me a coffee](https://www.buymeacoffee.com/jesty) to support device
  testing and future development.

Documentation: [architecture](docs/ARCHITECTURE.md) ·
[device validation](docs/DEVICE-VALIDATION.md) ·
[live results](docs/LIVE-RESULTS.md) ·
[benchmarks](docs/BENCHMARKS.md) ·
[release integrity](docs/RELEASE-INTEGRITY.md)

## License, provenance, and independence

- Source code and build scripts: [GPL-3.0](LICENSE).
- Jesty branding and project artwork: [ASSETS-LICENSE.md](ASSETS-LICENSE.md).
- Third-party names: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
- AI assistance: [full disclosure](AI_DISCLOSURE.md).

This is an independent community project and is not affiliated with or endorsed
by Retroid. Code, documentation, and visual assets were developed with
disclosed generative-AI assistance under the maintainer's direction,
supervision, review, and final approval. Hardware claims are based on physical
device evidence, not AI output alone.
