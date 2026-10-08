[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Before,
    [Parameter(Mandatory = $true)][string]$After
)

$ErrorActionPreference = 'Stop'
foreach ($path in @($Before, $After)) {
    if (-not (Test-Path -LiteralPath $path -PathType Container)) {
        throw "Capture directory not found: $path"
    }
}

$files = @(
    'settings-system.txt',
    'settings-global.txt',
    'settings-secure.txt',
    'candidate-app_whiteList.txt',
    'deviceidle-whitelist.txt',
    'interesting-settings.txt'
)

$changed = $false
foreach ($name in $files) {
    $left = Join-Path $Before $name
    $right = Join-Path $After $name
    if (-not (Test-Path $left) -or -not (Test-Path $right)) { continue }
    $diff = @(Compare-Object (Get-Content -LiteralPath $left) (Get-Content -LiteralPath $right))
    if ($diff.Count -eq 0) { continue }
    $changed = $true
    Write-Host "`n=== $name ==="
    $diff | Format-Table -AutoSize
}

if (-not $changed) {
    Write-Host 'No captured read-only surface changed between these samples.'
}
