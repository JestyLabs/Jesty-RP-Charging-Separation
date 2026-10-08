# PServerBinder client contract

Status: project research; no production transport change.

## Released contract

RootBridge looks up PServerBinder, uses transaction code 0 and writes String[]{command, "0"}. The reply is decoded with Parcel.createByteArray(). Keep this released convention; local stock-server investigation supports compatibility but does not establish all firmware behavior.

Transport acceptance and command success are separate. Current commands use fixed paths and validated numeric values. Any future interface should keep operations typed, bounded and validated rather than expose arbitrary user shell strings.

## Research launcher and replies

The manual probe submits only a fixed sh launcher path. Detach and redirection are performed inside the staged script. Multiline status is base64-wrapped into one line before decoding because the observed bridge returns only the first output line. Stage shell files with LF endings.

Do not add blind replay of ambiguous side-effecting writes. Reacquisition, serialization and new failure categories require a concrete failure and separate review. Binary/security details and captures stay local; only engineering conclusions are public.

## Evidence limits

A successful launch does not prove process survival, deep sleep scheduling, charging restoration or the cause of a Binder lifecycle failure. Our bounded probe and sentinel results are described in PServer-PROCESS-RESILIENCE.md. The helper has no charging authority.
