# Charging-separation benchmark notes

## 2026-09-26 Flip 2 capture

The signed public `1.3.0-dev` APK was installed on a USB-powered Retroid Pocket
Flip 2 running Android 13. The dashboard remained visible and the same cable,
screen state, workload, and ambient conditions were used for both states.

Each state contains 30 samples at approximately one-second intervals.

## Results

| Metric | Normal charging | Charging separated |
| --- | ---: | ---: |
| Android status | `Charging` | `Not charging` |
| Native charge limit | `0/10` | `10/10` |
| Battery current mean | +0.2388 A | -0.0111 A |
| Battery current median | +0.2498 A | 0.0000 A |
| Battery current range | +0.1514 to +0.2568 A | -0.0664 to 0.0000 A |
| Charge-counter change | +2,449 uAh | -109 uAh |
| USB input mean | 2.2862 W | 1.3792 W |
| Direct-to-device proxy mean | 1.3679 W | 1.4216 W |
| Battery temperature | 29.7 C | 29.7 C |

The similar direct-to-device proxy and reduced USB input are consistent with
normal device load continuing while the roughly 0.9 W charging component is
removed. This is a short functional capture, not a battery-health or thermal
longevity study.

The separated state showed a small average discharge rather than perfectly
zero current. Charging separation should therefore be described as stopping
active charging through the vendor's native path, not as guaranteeing that the
battery is electrically isolated under every workload.

## Raw data

- [`normal.csv`](benchmarks/2026-09-26/normal.csv)
- [`separated.csv`](benchmarks/2026-09-26/separated.csv)

The CSV files contain sample numbers and power telemetry only. Device serials,
account data, local paths, and raw logs are not included.
