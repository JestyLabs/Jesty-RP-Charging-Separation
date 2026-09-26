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
