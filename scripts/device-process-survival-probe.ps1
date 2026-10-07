[CmdletBinding()]
param(
    [ValidateSet('Start', 'Status', 'Stop', 'Clean')]
    [string]$Action = 'Status',
    [int]$DurationSeconds = 600
)

$ErrorActionPreference = 'Stop'
$package = 'com.jesty.rpchargingseparation'
$toolClass = 'com.jesty.rpchargingseparation.ProcessSurvivalProbeTool'

$adb = Get-Command adb.exe -ErrorAction SilentlyContinue
if (-not $adb) {
    throw 'adb.exe was not found in PATH.'
}

$state = (& $adb.Source get-state 2>$null | Select-Object -First 1)
if (($state | ForEach-Object { $_.Trim() }) -ne 'device') {
    throw 'No authorized Android device is connected through adb.'
}

$apkLine = (& $adb.Source shell pm path $package | Select-Object -First 1)
if (-not $apkLine) {
    throw "Package $package is not installed."
}
$apkLine = $apkLine.Trim()
if (-not $apkLine.StartsWith('package:')) {
    throw "Unexpected pm path output: $apkLine"
}
$apkPath = $apkLine.Substring('package:'.Length)

switch ($Action) {
    'Start' {
        $duration = [Math]::Max(60, [Math]::Min(1800, $DurationSeconds))
        $remote = "CLASSPATH='$apkPath' app_process / $toolClass start $duration"
    }
    'Status' {
        $remote = "CLASSPATH='$apkPath' app_process / $toolClass status"
    }
    'Stop' {
        $remote = "CLASSPATH='$apkPath' app_process / $toolClass stop"
    }
    'Clean' {
        $remote = "CLASSPATH='$apkPath' app_process / $toolClass clean"
    }
}

Write-Host "Probe action: $Action"
& $adb.Source shell $remote
if ($LASTEXITCODE -ne 0) {
    throw "Device probe action failed with exit code $LASTEXITCODE."
}
