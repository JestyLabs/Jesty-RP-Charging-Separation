[CmdletBinding()]
param(
    [ValidateSet('Start', 'Status', 'Stop', 'Clean')]
    [string]$Action = 'Status',
    [int]$DurationSeconds = 600
)

$ErrorActionPreference = 'Stop'
$package = 'com.jesty.rpchargingseparation'
$toolClass = 'com.jesty.rpchargingseparation.ProcessSurvivalProbeTool'
$remoteLauncher = '/data/local/tmp/jesty-rp-process-survival-launch.sh'

$adb = Get-Command adb.exe -ErrorAction SilentlyContinue
if (-not $adb) { throw 'adb.exe was not found in PATH.' }

$state = (& $adb.Source get-state 2>$null | Select-Object -First 1)
if (($state | ForEach-Object { $_.Trim() }) -ne 'device') {
    throw 'No authorized Android device is connected through adb.'
}

$apkLine = (& $adb.Source shell pm path $package | Select-Object -First 1)
if (-not $apkLine) { throw "Package $package is not installed." }
$apkLine = $apkLine.Trim()
if (-not $apkLine.StartsWith('package:')) { throw "Unexpected pm path output: $apkLine" }
$apkPath = $apkLine.Substring('package:'.Length)

function Invoke-ProbeTool([string]$Arguments) {
    $remote = "CLASSPATH='$apkPath' app_process / $toolClass $Arguments"
    & $adb.Source shell $remote
    if ($LASTEXITCODE -ne 0) {
        throw "Device probe action failed with exit code $LASTEXITCODE."
    }
}

switch ($Action) {
    'Start' {
        $duration = [Math]::Max(60, [Math]::Min(1800, $DurationSeconds))
        $temp = Join-Path ([System.IO.Path]::GetTempPath()) 'jesty-rp-process-survival-launch.sh'
        try {
            $scriptLines = & $adb.Source shell "CLASSPATH='$apkPath' app_process / $toolClass print-launcher $duration"
            if ($LASTEXITCODE -ne 0 -or -not $scriptLines) {
                throw 'Could not generate the device-side launcher script.'
            }
            [System.IO.File]::WriteAllLines($temp, [string[]]$scriptLines,
                    (New-Object System.Text.UTF8Encoding($false)))
            & $adb.Source push $temp $remoteLauncher | Out-Host
            if ($LASTEXITCODE -ne 0) { throw 'Could not stage launcher script.' }
            & $adb.Source shell chmod 700 $remoteLauncher
            if ($LASTEXITCODE -ne 0) { throw 'Could not chmod launcher script.' }
            Invoke-ProbeTool 'start'
            Write-Host "Staged bounded launcher for $duration seconds."
        } finally {
            Remove-Item -LiteralPath $temp -Force -ErrorAction SilentlyContinue
        }
    }
    'Status' { Invoke-ProbeTool 'status' }
    'Stop'   { Invoke-ProbeTool 'stop' }
    'Clean'  { Invoke-ProbeTool 'clean' }
}
