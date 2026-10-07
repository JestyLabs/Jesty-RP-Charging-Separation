[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$files = @(
    (Join-Path $PSScriptRoot 'capture-retroid-process-protection.ps1'),
    (Join-Path $PSScriptRoot 'compare-retroid-process-protection.ps1')
)

$forbidden = @(
    '(?i)settings\s+(put|delete|reset)',
    '(?i)deviceidle\s+whitelist\s+[+-]',
    '(?i)\bam\s+force-stop\b',
    '(?i)\bpm\s+(disable|enable|uninstall|clear)\b',
    '(?i)\bsetprop\b',
    '(?i)charge_control',
    '(?i)>\s*/sys/'
)

foreach ($file in $files) {
    $text = Get-Content -LiteralPath $file -Raw
    foreach ($pattern in $forbidden) {
        if ($text -match $pattern) {
            throw "Read-only capture script contains forbidden mutation pattern '$pattern': $file"
        }
    }
}

Write-Host 'Retroid process-protection capture scripts are read-only by static guard.'
