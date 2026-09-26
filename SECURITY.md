# Security and safety policy

Do not open a public issue containing signing material, passwords, tokens,
private paths, device serials, or a full diagnostic archive. Redact personal data
before sharing logs.

For safety reports, include only the device model, firmware version, app
version, charger type, relevant redacted telemetry, and the exact sequence that
triggered the problem.

If charging restoration is not confirmed:

1. Disconnect external power.
2. Reboot the device.
3. Reopen the app and leave charging separation disabled.
4. Confirm that `charge_control_limit` reads `0` before reconnecting a charger.

Do not repeatedly retry privileged writes when restoration is uncertain.
