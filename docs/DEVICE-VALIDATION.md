# Device validation checklist

Record the exact model, firmware build, charger, cable, battery percentage, and
ambient conditions. Redact serial numbers and account information.

- [ ] Normal charging is confirmed before enabling.
- [ ] Enable reaches `Not charging` and `limit == limitMax`.
- [ ] Battery current settles within the confirmation threshold.
- [ ] USB input and estimated device power remain plausible under load.
- [ ] A sustained unsafe battery discharge disables separation.
- [ ] Manual disable restores `charge_control_limit` to `0`.
- [ ] Closing the dashboard and removing it from recents preserves the active
  foreground service.
- [ ] Unplug and replug transitions between ARMED and ACTIVE safely.
- [ ] Reboot with boot restoration OFF remains normal.
- [ ] Reboot with boot restoration ON reconciles correctly after USB is present.
- [ ] Force stop behavior and recovery after opening the app are documented.
- [ ] The device charges normally after all tests.

## Pocket Mini V2 1.4.1 compatibility check

Status: pending an owner test of the exact signed 1.4.1 APK.

- [ ] Diagnostic line identifies the Pocket Mini V2 model.
- [ ] App reads `charge_control_limit` and `charge_control_limit_max`.
- [ ] Enabling reaches `CHARGING SEPARATED`, `Not charging`, and matching
  current/maximum limit values while USB input remains above zero.
- [ ] Battery flow remains near neutral rather than sustaining discharge.
- [ ] Unplug/replug returns to the armed state and re-enables safely.
- [ ] Disabling restores limit `0` and normal `Charging` behavior.
- [ ] A normal reboot with restore disabled leaves normal charging active.

Do not mark the Mini V2 as validated from chipset similarity or a successful
install alone; the charging state, power flow, and restoration checks must pass.
