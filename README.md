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
<a href="https://github.com/JestyLabs/Jesty-RP-Charging-Separation/releases/latest"><strong>Download latest APK</strong></a>
· <a href="#how-it-works">How it works</a>
· <a href="#measured-results">Measured results</a>
· <a href="https://www.buymeacoffee.com/jesty">☕ Support</a>
</p>

<p align="center">
<img alt="Retroid Pocket Flip 2" src="https://img.shields.io/badge/compatible-Flip%202-7C3AED?style=for-the-badge">
<img alt="Retroid Pocket 5" src="https://img.shields.io/badge/compatible-RP5-7C3AED?style=for-the-badge">
<img alt="Retroid Pocket Mini" src="https://img.shields.io/badge/compatible-Mini-7C3AED?style=for-the-badge">
<img alt="Retroid Pocket Mini V2" src="https://img.shields.io/badge/compatible-Mini%20V2-7C3AED?style=for-the-badge">
<img alt="Android 13" src="https://img.shields.io/badge/Android-13-3DDC84?style=for-the-badge&logo=android&logoColor=white">
<img alt="No Magisk or rooting" src="https://img.shields.io/badge/setup-no%20Magisk%20%2F%20rooting-16A34A?style=for-the-badge">
<img alt="GPL 3" src="https://img.shields.io/badge/code-GPL--3.0-8B5CF6?style=for-the-badge">
</p>

<p align="center">
<img src="https://github.com/user-attachments/assets/0e4346b9-dc09-4348-b414-1398aeac9f4b" alt="v1.5.2 charging toward 80 percent on Retroid Pocket Flip 2" width="960">
</p>

---

### What the app does

Normally, USB powers the handheld **and** charges the battery at the same time.  
Charging Separation uses Retroid's built-in privileged controls to stop active battery charging while USB continues powering the device.

- No Magisk, root, Termux or ADB required
- Live battery current, USB input, direct-to-device estimate and temperature
- **Right away** or **At a battery level** (default: stop at 80%, charge again at 70%)
- Verifies that separation actually activated before reporting success
- Automatic safety fallback to normal charging if something looks wrong
- Optional restore after reboot
- In-app updates
- Runs in the background - you can close the app

The app does **not** physically disconnect the battery, so small positive or negative currents can still appear depending on load, charger, firmware and temperature.

---

### How it works

| | Normal charging | Charging Separation |
|---|-----------------|---------------------|
| USB connected | Yes | **Yes** |
| Handheld remains powered | Yes | **Yes** |
| Battery actively charging | Yes | **No** |
| Android battery state | `Charging` | **`Not charging`** |
| Live safety monitoring | - | **Yes** |
| App must stay open | - | **No** |
| Root / Magisk needed | - | **No** |

---

### Measured results (Retroid Pocket Flip 2)

Controlled comparison, 30 one-second samples per state, same cable, screen state and workload:

| Metric | Normal charging | Charging separated |
|--------|----------------:|-------------------:|
| Android state | `Charging` | **`Not charging`** |
| Native limit | 0/10 | **10/10** |
| Average battery current | +0.239 A | **-0.011 A** |
| Charge-counter change | +2,449 µAh | **-109 µAh** |
| Average USB input | 2.286 W | **1.379 W** |
| Battery temperature | 29.7 °C | 29.7 °C |

This is a short functional measurement, **not** a battery-health or longevity study.  
Full method and samples → [benchmarks](docs/BENCHMARKS.md)

---

### Set a battery threshold

1. Enable **Bypass charging**
2. Choose **At a battery level**
3. Set **Stop charging at** (default 80%)
4. Set **Charge again at** (default 70%)

The two levels stay at least 5 percentage points apart.  
Choose **Right away** if you want separation as soon as USB is connected.

---

### Compatibility

| Device | Status |
|--------|--------|
| **Retroid Pocket Flip 2** | Compatible (measured + screenshots) |
| **Retroid Pocket 5** | Compatible (maintainer + community) |
| **Retroid Pocket Mini** | Compatible (community) |
| **Retroid Pocket Mini V2** | Compatible (community) |
| Other Retroid models | Not validated |
| Unrelated Android devices | Unsupported |

Compatibility is based on the required Retroid control nodes and privileged bridge, not a simple model-name list.

---

### Safety behavior

Before and while separation is active the app:

- verifies the charging-control capability
- confirms Android reports `Not charging`
- monitors USB input, battery current, charge counter and temperature
- restores normal charging if validation or telemetry fails
- disables separation after repeated evidence that the battery may be powering the device instead of USB

---

### How to use

1. [Download the latest APK](https://github.com/JestyLabs/Jesty-RP-Charging-Separation/releases/latest)
2. Install and open **Jesty RP Charging Separation**
3. Connect a suitable USB charger
4. Enable **Bypass charging** and confirm `Not charging` in the live diagnostic
5. (Optional) Enable **Maintain bypass charging after reboot**

After that you can close the app. The controller continues running in the background.

**Note:** Android **Force stop** (or the vendor launcher's "Clear all") blocks the app until you open it again. An individual swipe from Recents does not.

---

### Support the project

Free and open source. No features locked behind donations.

<p align="center">
  <a href="https://www.buymeacoffee.com/jesty">
    <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me a Coffee" width="217">
  </a>
</p>

- ⭐ Star the repository so other Retroid owners can find it
- 🧪 Share carefully redacted results from another firmware
- 🐛 Report reproducible safety or compatibility issues

---

### Documentation

[Architecture](docs/ARCHITECTURE.md) · 
[Device validation](docs/DEVICE-VALIDATION.md) · 
[Live results](docs/LIVE-RESULTS.md) · 
[Benchmarks](docs/BENCHMARKS.md) · 
[Release integrity](docs/RELEASE-INTEGRITY.md) · 
[Research provenance](PROVENANCE.md)

<details>
<summary><strong>Technical implementation</strong></summary>

The app reaches Retroid's hidden `PServerBinder` through Android's service manager. The vendor transaction accepts a two-element String array and runs a narrowly scoped command through Retroid's privileged `pservice` path.

See [Architecture](docs/ARCHITECTURE.md) for the state machine and failure behavior.

</details>

<details>
<summary><strong>Build from source</strong></summary>

Requirements: PowerShell 5.1+, Android SDK platform 28+, Android Build Tools 34+, JDK 17.

```powershell
.\build.ps1
