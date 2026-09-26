<p align="center">
  <img src="assets/branding/jesty_wordmark_header.png" alt="Jesty" width="390">
</p>

<h1 align="center">Jesty RP Charging Separation</h1>

<p align="center">
  <strong>Play while plugged in without continuously charging the battery.</strong><br>
  Uses Retroid's own charging controls on the Pocket 5 and Pocket Flip 2, with live battery/USB monitoring and automatic safety checks.
</p>

<p align="center">
  <img alt="Retroid Pocket Flip 2" src="https://img.shields.io/badge/tested-Flip%202-7C3AED?style=for-the-badge">
  <img alt="Retroid Pocket 5" src="https://img.shields.io/badge/tested-RP5-F59E0B?style=for-the-badge">
  <img alt="Android 13" src="https://img.shields.io/badge/Android-13-3DDC84?style=for-the-badge&amp;logo=android&amp;logoColor=white">
  <img alt="GPL 3" src="https://img.shields.io/badge/code-GPL--3.0-8B5CF6?style=for-the-badge">
</p>

<p align="center">
  <a href="https://github.com/JestyLabs/Jesty-RP-Charging-Separation/releases/tag/v1.3.0-dev"><strong>Download the signed development pre-release</strong></a>
  · <a href="docs/DEVICE-VALIDATION.md">Safety checklist</a>
  · <a href="docs/BENCHMARKS.md">Measurements and raw data</a>
  · <a href="https://www.buymeacoffee.com/jesty">☕ Support development</a>
</p>

<p align="center">
  <img src="docs/images/dashboard-separated.png" alt="Jesty RP Charging Separation active on Retroid Pocket Flip 2" width="100%">
</p>

> [!WARNING]
> **Made for compatible Retroid handhelds.** This is an unofficial community
> project and is not made, supported, or endorsed by Retroid. It uses
> firmware-dependent charging controls, so read the safety notes before enabling
> it on a device or firmware version that has not been tested.

## What does it do?

Normally, when you plug your Retroid into USB, the charger does two things at
the same time:

1. powers the handheld;
2. charges the battery.

That is fine most of the time. But during a long gaming session, docked setup,
or overnight use, you may want the charger to keep powering the handheld
without continuously pushing charge into the battery.

**Jesty RP Charging Separation lets compatible Retroid devices stop actively
charging the battery while USB remains connected and continues supplying the
running handheld.**

You can turn it on from the app, see what the battery and charger are doing in
real time, and turn normal charging back on whenever you want.

| | Normal charging | Charging separation |
| --- | --- | --- |
| USB connected | Yes | **Yes** |
| Handheld keeps running from external power | Yes | **Yes** |
| Battery actively charging | Yes | **No** |
| Android battery state | `Charging` | **`Not charging`** |
| Live battery / USB monitoring | Yes | **Yes** |
| Automatic safety checks | — | **Yes** |

### See both states

| Charging separated | Normal charging |
| --- | --- |
| ![Charging separated dashboard](docs/images/dashboard-separated.png) | ![Normal charging dashboard](docs/images/dashboard-normal.png) |

## Why use it?

- **Play plugged in without continuously charging the battery.**
- **Uses Retroid's own charging controls** instead of a generic Android battery hack.
- **No Magisk setup or terminal commands required.**
- **Shows live battery current, USB power, temperature and charging state.**
- **Checks that charging separation actually activated** before reporting success.
- **Automatically restores normal charging if the safety checks fail.**
- **Can restore your preferred state after a normal reboot** if you enable that option.
- **Works without keeping the dashboard open** while separation is active.

> [!NOTE]
> Charging separation does **not** mean the battery is physically disconnected.
> Small positive or negative battery currents can still appear depending on load,
> charger, firmware and battery state. The goal is to stop active charging while
> external power continues supplying the handheld.

## Supported devices

| Device | Current evidence |
| --- | --- |
| **Retroid Pocket Flip 2** | Public APK installed, measured and repeatedly tested |
| **Retroid Pocket 5** | Successfully tested; more repeat telemetry is welcome |
| Other Retroid models | Not validated yet — treat as unsupported until tested |
| Unrelated Android devices | Unsupported |

The app checks for the Retroid firmware controls it needs before enabling
charging separation.

For technical reference, those controls include:

```text
/sys/class/power_supply/battery/charge_control_limit
/sys/class/power_supply/battery/charge_control_limit_max
```

The app also relies on Retroid's privileged `PServerBinder` service.

## Measured on a Retroid Pocket Flip 2

A controlled comparison captured 30 one-second samples per state using the same
USB cable, screen state, workload and ambient conditions:

| Metric | Normal charging | Charging separated |
| --- | ---: | ---: |
| Android state | `Charging` | **`Not charging`** |
| Native limit | `0/10` | **`10/10`** |
| Average battery current | +0.239 A | **-0.011 A** |
| Charge-counter change | +2,449 uAh | **-109 uAh** |
| Average USB input | 2.286 W | **1.379 W** |
| Direct-to-device proxy | 1.368 W | 1.422 W |
| Battery temperature | 29.7 C | 29.7 C |

The important part is the overall behavior: when charging separation was
enabled, Android changed to **Not charging**, battery current dropped to roughly
zero, and the handheld continued running with USB connected.

The similar direct-to-device power estimate is consistent with USB continuing
to supply the handheld while the charging component disappeared.

> [!NOTE]
> This is a short functional test, not a battery-health or battery-longevity
> study. Charger, workload, firmware, temperature and battery state can all
> change the numbers. See [docs/BENCHMARKS.md](docs/BENCHMARKS.md) for the full
> methodology and sanitized raw samples.

## Built-in safety checks

The app does more than simply flip a setting.

Before and while charging separation is active, it:

- checks that the expected Retroid charging controls are available;
- verifies the new charging limit after changing it;
- confirms Android reports `Not charging`;
- monitors USB input, battery current, charge counter and temperature;
- restores normal charging if validation or telemetry fails;
- automatically disables separation after repeated samples indicating that the
  battery may be powering the handheld instead of USB.

It does **not** change CPU governors, CPU frequencies, display settings or
unrelated Android system properties.

## Installation

1. Download `Jesty-RP-Charging-Separation-1.3.0-dev.apk` from the
   [GitHub release](https://github.com/JestyLabs/Jesty-RP-Charging-Separation/releases/tag/v1.3.0-dev).
2. Install and open **Jesty RP Charging Separation**.
3. Connect a suitable USB charger.
4. Enable **Charging separation**.
5. Confirm the app reports `Not charging` and sensible battery/USB readings.

The public package is `com.jesty.rpchargingseparation`. It installs separately
from private GracaBlockBat development builds created before the public rebrand.

## Everyday behavior

- While active or armed, the controller runs as an Android foreground service.
- Closing the dashboard or removing it from recents does not normally stop it.
- **Restore after a normal reboot** is optional.
- Turning charging separation off restores normal charging.
- A safety failure also restores normal charging automatically.

**Android Settings -> Force stop is different.** Force stop prevents the app and
its boot receiver from running again until the app is opened manually. Do not
use Force stop as the normal way to leave charging separation enabled.

<details>
<summary><strong>Technical implementation</strong></summary>

The app calls Android's hidden service manager to reach Retroid's
`PServerBinder`. The vendor transaction accepts a two-element String array and
runs a narrowly scoped command through Retroid's privileged `pservice` path.

Before reporting success, the app verifies the control node, reads back the
limit, checks Android's battery status, and starts continuous safety telemetry.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the protocol, state machine
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

## Support and contribute

**Jesty RP Charging Separation is free and open source.**

If the project is useful to you, there are several ways you can help:

- ⭐ **Star the repository** so other Retroid owners can find it.
- 🧪 **Test another firmware or compatible Retroid device** and share carefully redacted results.
- 🐛 **Report bugs or compatibility issues.**
- 💡 **Suggest improvements** or contribute code/documentation.
- ☕ **[Buy me a coffee](https://www.buymeacoffee.com/jesty)** to help fund
  additional device testing, safety work, firmware compatibility and future updates.

Testing and compatibility reports are just as valuable as financial support.

<p align="center">
  <a href="https://www.buymeacoffee.com/jesty">
    <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me a Coffee" width="217">
  </a>
</p>

When reporting a compatibility issue, please include the exact device, firmware,
charger and carefully redacted telemetry. Never upload a keystore, password,
device serial, account email or unreviewed log bundle.

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
