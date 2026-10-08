[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9._-]+$')]
    [string]$Label,
    [string]$OutputRoot
)

$ErrorActionPreference = 'Stop'
$package = 'com.jesty.rpchargingseparation'
$adb = Get-Command adb.exe -ErrorAction SilentlyContinue
if (-not $adb) { throw 'adb.exe was not found in PATH.' }

$state = (& $adb.Source get-state 2>$null | Select-Object -First 1)
if (($state | ForEach-Object { $_.Trim() }) -ne 'device') {
    throw 'No authorized Android device is connected through adb.'
}

if (-not $OutputRoot) {
    $repoRoot = Split-Path -Parent $PSScriptRoot
    $OutputRoot = Join-Path $repoRoot 'build\retroid-process-protection'
}
$captureDir = Join-Path $OutputRoot $Label
if (Test-Path -LiteralPath $captureDir) {
    throw "Capture already exists: $captureDir. Use a new label so evidence is not overwritten."
}
New-Item -ItemType Directory -Path $captureDir -Force | Out-Null

function Save-Lines([string]$Name, [scriptblock]$Command) {
    $lines = @(& $Command) | ForEach-Object { [string]$_ }
    $path = Join-Path $captureDir $Name
    [System.IO.File]::WriteAllLines($path, $lines, (New-Object System.Text.UTF8Encoding($false)))
    return $lines
}

$meta = @(
    "captured_utc=$([DateTime]::UtcNow.ToString('o'))",
    "label=$Label",
    "package=$package",
    "model=$((& $adb.Source shell getprop ro.product.model).Trim())",
    "device=$((& $adb.Source shell getprop ro.product.device).Trim())",
    "display_id=$((& $adb.Source shell getprop ro.build.display.id).Trim())",
    "fingerprint=$((& $adb.Source shell getprop ro.build.fingerprint).Trim())"
)
[System.IO.File]::WriteAllLines((Join-Path $captureDir 'meta.txt'), $meta,
        (New-Object System.Text.UTF8Encoding($false)))

$system = Save-Lines 'settings-system.txt' { & $adb.Source shell settings list system }
$global = Save-Lines 'settings-global.txt' { & $adb.Source shell settings list global }
$secure = Save-Lines 'settings-secure.txt' { & $adb.Source shell settings list secure }
Save-Lines 'candidate-app_whiteList.txt' { & $adb.Source shell settings get system app_whiteList } | Out-Null
Save-Lines 'deviceidle-whitelist.txt' { & $adb.Source shell cmd deviceidle whitelist } | Out-Null

$interesting = @($system + $global + $secure) | Where-Object {
    $_ -match [regex]::Escape($package) -or
    $_ -match '(?i)white.?list|clean|standby|ignore|process|idle'
} | Sort-Object -Unique
[System.IO.File]::WriteAllLines((Join-Path $captureDir 'interesting-settings.txt'),
        [string[]]$interesting, (New-Object System.Text.UTF8Encoding($false)))

Write-Host "Read-only capture saved to: $captureDir"
Write-Host 'No device setting was modified.'
