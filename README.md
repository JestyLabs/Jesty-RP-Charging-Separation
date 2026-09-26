<div align="center">

# Jesty RP Charging Separation

**A safety-first controller and live power dashboard for Retroid's native
charging-separation path.**

[Support Jesty on Buy Me a Coffee](https://buymeacoffee.com/jesty)

</div>

> [!WARNING]
> This is an unofficial community project. It changes a vendor charging-control
> node through Retroid's privileged `PServerBinder`. Unsupported hardware,
> firmware changes, or interrupted restoration can affect charging behavior.
> Read the limitations and validate telemetry on your own device.

> [!NOTE]
> This project was developed with AI assistance. Parts of the code,
> documentation, UI, and visual assets were generated or refined with
> generative AI under the maintainer's direction. AI output is not treated as
> hardware-validation evidence. See [AI_DISCLOSURE.md](AI_DISCLOSURE.md).

## Dashboard preview

| Charging separated | Normal charging |
| --- | --- |
| ![Charging separated dashboard](docs/images/dashboard-separated.png) | ![Normal charging dashboard](docs/images/dashboard-normal.png) |

## The problem

Some Retroid firmware exposes a native charging-control pair:

```text
/sys/class/power_supply/battery/charge_control_limit
/sys/class/power_supply/battery/charge_control_limit_max
```

On compatible devices, setting the active limit to the firmware's maximum can
place the charger in its native `Not charging` state while external USB power
continues feeding the device. This reduces sustained battery charging during a
long docked or handheld session.

This is not a generic Android battery-bypass hack. The app only proceeds when
the expected Retroid nodes are readable and the vendor privileged service is
available.

## What the app does

- Checks for the native `charge_control_limit` controls.
- Sends the narrowly scoped write through `PServerBinder`.
- Reads the value back and requires Android to report `Not charging`.
- Monitors USB input, battery current, estimated direct device power, battery
  temperature, and the charge counter.
- Fails closed and restores the normal limit (`0`) if validation or telemetry
  fails.
- Automatically disables if the battery appears to power the device for three
  consecutive samples while separation is active.
- Can remember the requested state across a normal reboot when the user enables
  the boot option.

## Measured behavior on Flip 2

A short controlled comparison captured 30 one-second samples per state on a
USB-powered Retroid Pocket Flip 2:

| Metric | Normal charging | Charging separated |
| --- | ---: | ---: |
| Android battery state | `Charging` | `Not charging` |
| Native limit | `0/10` | `10/10` |
| Average battery current | +0.239 A | -0.011 A |
| Charge-counter change | +2,449 uAh | -109 uAh |
| Average USB input | 2.286 W | 1.379 W |
| Battery temperature | 29.7 C | 29.7 C |

The small negative separated current shows that zero battery flow is not
guaranteed at every instant; load, charger, firmware, and telemetry update
timing still matter. The important observed transition was from active charging
to the firmware's native `Not charging` state while USB continued supplying the
device. See [docs/BENCHMARKS.md](docs/BENCHMARKS.md) for raw samples and
limitations.

## Persistence and stopping behavior

The controller runs as an Android foreground service while active or armed.
Closing the dashboard or removing it from recents does not normally stop the
service.

**Android Settings -> Force stop is different.** Force stop prevents the app
and its boot receiver from running again until the user opens the app. Do not
treat Force stop as a normal way to leave charging separation enabled.

The app restores normal charging when its active service is destroyed, when the
user disables separation, or when its safety checks fail. A hard crash, firmware
bug, or device power loss still needs real-device verification; software cannot
promise recovery from every failure mode.

## Compatibility status

| Device / path | Current evidence |
| --- | --- |
| Retroid `PServerBinder` transport | Binder transaction format confirmed on real Retroid firmware |
| Devices exposing both limit nodes | Capability detection implemented; no model-name whitelist |
| Retroid Pocket Flip 2 | Original development target; repeat release validation required after the public rebrand |
| Retroid Pocket 5 | Successfully tested by the maintainer on a friend's RP5; a sanitized repeat capture is still wanted for publication |
| Other Android devices | Unsupported unless they expose the same vendor service and semantics |

Until the RP5 validation checklist is completed, do not describe RP5 support as
confirmed merely because the sysfs files exist.

## Build

Requirements:

- Windows PowerShell 5.1 or newer
- Android SDK platform 28 or newer
- Android SDK Build Tools 34 or newer
- JDK 17 (the project emits Java 8-compatible bytecode)

Build an unsigned APK:

```powershell
.\build.ps1
```

Specify non-default tool locations when required:

```powershell
.\build.ps1 -AndroidSdk C:\Android\Sdk -JdkHome C:\Java\jdk-17
```

Signing is deliberately opt-in and reads the password interactively:

```powershell
.\build.ps1 -Keystore C:\secure\jesty-release.jks -KeyAlias release
```

Keys, passwords, APKs, device captures, and local SDK paths do not belong in
Git. The public package is `com.jesty.rpchargingseparation`; it installs
separately from private development packages created before the public rebrand.

## State-aware artwork

The dashboard uses the approved Jesty wordmark and changes the device scene to
match confirmed hardware state: the charging-separation artwork appears only
after separation is validated; normal charging, armed, and failure states use
the battery-charging scene. See [assets/README.md](assets/README.md) and
[docs/PUBLICATION-CHECKLIST.md](docs/PUBLICATION-CHECKLIST.md).

## License

- Code and build scripts: [GPL-3.0](LICENSE).
- Jesty name, mascot, wordmark, and project artwork: separate terms in
  [ASSETS-LICENSE.md](ASSETS-LICENSE.md).
- Third-party Android and Retroid names remain the property of their respective
  owners; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

This project is not affiliated with or endorsed by Retroid.
