# Jesty RP Charging Separation 1.5.8

Automatic mode now keeps the CPU awake only while charging toward the selected
stop level. This lets the threshold monitor keep checking when the screen is
off. The wake lock is released when bypass starts, USB disconnects, bypass is
disabled, or the service ends.

On a Flip 2 with firmware 1.0.0.130, the exact signed APK held the lock with
the screen asleep, released it when bypass activated, and reacquired it after
a controlled process crash. Settings were restored to 80%/65% afterward.
The reported full charge cycle on stated firmware 1.0.0.311 still needs
retesting; the full build ID has not been independently verified.
[Issue #2](https://github.com/JestyLabs/Jesty-RP-Charging-Separation/issues/2)
remains open for the reporter's result.

APK: `Jesty-RP-Charging-Separation-1.5.8.apk`

SHA-256: `235FF8FA1A460F9542D39DE9907580A881BABF9045D19731C963F276AD175CE5`
