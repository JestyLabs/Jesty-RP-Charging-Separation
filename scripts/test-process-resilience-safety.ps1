[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$package = 'src\com\jesty\rpchargingseparation'
$targets = @(
    (Join-Path $root "$package\ProcessSurvivalProbe.java"),
    (Join-Path $root "$package\ProcessSurvivalProbeCommand.java"),
    (Join-Path $root "$package\ProcessSurvivalProbeTool.java"),
    (Join-Path $root "$package\RestoreOnlyWatchdogPolicy.java"),
    (Join-Path $root "$package\RestoreWatchdogSentinelScript.java"),
    (Join-Path $root "$package\RestoreRetryPolicy.java"),
    (Join-Path $root 'scripts\device-process-survival-probe.ps1'),
    (Join-Path $root 'scripts\device-watchdog-sentinel.ps1')
)

$forbidden = @(
    '(?i)charge_control',
    '(?i)/sys/class/power_supply',
    '(?i)settings\s+(put|delete|reset)',
    '(?i)deviceidle\s+whitelist\s+[+-]',
    '(?i)\bsetprop\b',
    '(?i)\bam\s+force-stop\b',
    '(?i)\bpm\s+(disable|enable|uninstall|clear)\b'
)

foreach ($file in $targets) {
    $text = Get-Content -LiteralPath $file -Raw
    foreach ($pattern in $forbidden) {
        if ($text -match $pattern) {
            throw "Process-resilience research escaped its no-charging/no-policy-mutation boundary: '$pattern' in $file"
        }
    }
}

Write-Host 'Process-resilience research stays outside charging and vendor-policy mutation.'
