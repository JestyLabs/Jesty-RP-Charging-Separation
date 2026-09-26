<p align="center">
  <img src="assets/branding/jesty_wordmark_header.png" alt="Jesty" width="390">
</p>

<h1 align="center">Jesty RP Charging Separation</h1>

<p align="center">
  <strong>Use Retroid's native charging-separation path and see where the power is going.</strong><br>
  Designed for Retroid Pocket Flip 2 and Retroid Pocket 5.
</p>

<p align="center">
  <img alt="Retroid Pocket Flip 2" src="https://img.shields.io/badge/tested-Flip%202-7C3AED?style=for-the-badge">
  <img alt="Retroid Pocket 5" src="https://img.shields.io/badge/tested-RP5-F59E0B?style=for-the-badge">
  <img alt="Android 13" src="https://img.shields.io/badge/Android-13-3DDC84?style=for-the-badge&amp;logo=android&amp;logoColor=white">
  <img alt="GPL 3" src="https://img.shields.io/badge/code-GPL--3.0-8B5CF6?style=for-the-badge">
</p>

<p align="center">
  <a href="https://github.com/SirJesty/Jesty-RP-Charging-Separation/releases/tag/v1.3.0-dev"><strong>Download the signed development pre-release</strong></a>
  · <a href="docs/DEVICE-VALIDATION.md">Safety checklist</a>
  · <a href="docs/BENCHMARKS.md">Measurements and raw data</a>
</p>

<p align="center">
  <img src="docs/images/dashboard-separated.png" alt="Jesty RP Charging Separation active on Retroid Pocket Flip 2" width="100%">
</p>

> [!WARNING]
> This is an unofficial community project. It controls a vendor charging node
> through Retroid's privileged `PServerBinder`. Firmware changes or unsupported
> hardware can change that behavior. Read the safety notes before enabling it.

## What charging separation means

Normally, plugging in USB powers the device **and charges the battery**. During
long docked or handheld sessions that can keep adding charge and heat when you
mainly want external power.

On compatible Retroid firmware, charging separation asks the native charging
controller to stop actively charging the battery while USB continues supplying
the running device.

| | Normal charging | Charging separated |
| --- | --- | --- |
| Android battery state | `Charging` | **`Not charging`** |
| Native charge limit | `0/10` | **`10/10`** |
| USB still connected | yes | **yes** |
| App monitors battery flow | yes | **yes, with automatic safety checks** |

This is not a generic Android battery hack and it does not claim to physically
disconnect the battery. Small positive or negative battery currents can still
appear as load and firmware conditions change.

### See both states

| Charging separated | Normal charging |
| --- | --- |
| ![Charging separated dashboard](docs/images/dashboard-separated.png) | ![Normal charging dashboard](docs/images/dashboard-normal.png) |

## Supported devices

| Device | Current evidence |
| --- | --- |
| **Retroid Pocket Flip 2** | Public APK installed and measured; enable/disable and restoration confirmed |
| **Retroid Pocket 5** | Successfully tested by the maintainer on a friend's RP5; sanitized repeat telemetry is still wanted |
| Other Retroid models | Capability detection is used instead of a model-name whitelist; treat unmeasured devices as unsupported |
| Unrelated Android devices | Unsupported |

The required firmware controls are:

```text
/sys/class/power_supply/battery/charge_control_limit
/sys/class/power_supply/battery/charge_control_limit_max
```

The app also requires Retroid's privileged `PServerBinder` service. Merely
having similarly named files is not sufficient proof of compatibility.

## Measured on a Retroid Pocket Flip 2

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

The similar direct-to-device proxy is consistent with the device continuing to
run from USB while the roughly 0.9 W charging component disappeared. The small
negative separated current is also why the app says **charging separation**, not
“perfect battery isolation.”

> [!NOTE]
> This is a short functional measurement, not a battery-health or longevity
> study. Chargers, firmware, workload, temperature, and battery state can change
> the numbers. See [docs/BENCHMARKS.md](docs/BENCHMARKS.md) for methodology and
> sanitized raw samples.

## What the app protects against

- Verifies that the expected native limit controls are readable.
- Uses the narrow Retroid Binder command instead of general-purpose root.
- Reads the value back and requires Android to report `Not charging`.
- Monitors USB input, battery current, charge counter, temperature, and an
  estimated direct-to-device power figure.
- Fails closed and restores normal charging if validation or telemetry fails.
- Automatically disables after three consecutive samples where the battery
  appears to be powering the device while separation is active.
- Never changes CPU governors, frequencies, display composer settings, or
  unrelated system properties.

## Installation

1. Download `Jesty-RP-Charging-Separation-1.3.0-dev.apk` from the
   [GitHub release](https://github.com/SirJesty/Jesty-RP-Charging-Separation/releases/tag/v1.3.0-dev).
2. Install and open **Jesty RP Charging Separation**.
3. Connect a suitable USB charger and review the live telemetry.
4. Enable **Charging separation**.
5. Confirm `Not charging`, limit `10/10`, and sensible battery/USB readings.

The public package is `com.jesty.rpchargingseparation`. It installs separately
from private GracaBlockBat development builds created before the public rebrand.

## Persistence and stopping behavior

- While active or armed, the controller runs as an Android foreground service.
- Closing the dashboard or removing it from recents does not normally stop that service.
- **Restore after a normal reboot** is opt-in.
- Disabling separation, a safety failure, or normal service destruction restores
  charge limit `0`.

**Android Settings -> Force stop is different.** Force stop prevents the app
and its boot receiver from running again until the user opens the app. Do not
use Force stop as a normal way to leave charging separation enabled.

<details>
<summary><strong>Technical implementation</strong></summary>

The app calls Android's hidden service manager to reach Retroid's
`PServerBinder`. The vendor transaction accepts a two-element String array and
runs a narrowly scoped command through Retroid's privileged `pservice` path.

Before reporting success, the app verifies the control node, reads back the
limit, checks Android's battery status, and starts continuous safety telemetry.
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the protocol, state machine,
and failure behavior.

</details>

<details>
<summary><strong>Building from source</strong></summary>

Requirements: PowerShell 5.1+, Android SDK platform 28+, Android Build Tools
34+, and JDK 17.

```powershell
.\build.ps1
```

Signing is deliberately opt-in and reads the password interactively. Keys,
passwords, APKs, device captures, and local SDK paths do not belong in Git.

</details>

## Support the project

If this makes long Retroid sessions easier, a coffee helps fund device testing,
safety work, documentation, and future compatibility updates.

<p align="center">
  <a href="https://www.buymeacoffee.com/jesty">
    <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me a Coffee" width="217">
  </a>
</p>

Please include the exact device, firmware, charger, and carefully redacted
telemetry when reporting a compatibility issue. Never upload a keystore,
password, device serial, account email, or unreviewed log bundle.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Device validation checklist](docs/DEVICE-VALIDATION.md)
- [Live results](docs/LIVE-RESULTS.md)
- [Benchmarks and raw samples](docs/BENCHMARKS.md)
- [Release integrity](docs/RELEASE-INTEGRITY.md)
- [AI assistance disclosure](AI_DISCLOSURE.md)

## License and credits

- Source code and build scripts: [GPL-3.0](LICENSE).
- Jesty name, mascot, wordmark, and project artwork: [ASSETS-LICENSE.md](ASSETS-LICENSE.md).
- Third-party Android and Retroid names: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

This project is not affiliated with or endorsed by Retroid. It was developed
with disclosed generative-AI assistance under the maintainer's direction.
