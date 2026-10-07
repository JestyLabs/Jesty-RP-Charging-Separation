[CmdletBinding()]
param(
    [ValidateSet('Start', 'Status', 'Stop', 'Clean')]
    [string]$Action = 'Status',
    [int]$LeaseSeconds = 30
)

$ErrorActionPreference = 'Stop'
$package = 'com.jesty.rpchargingseparation'
$toolClass = 'com.jesty.rpchargingseparation.ProcessSurvivalProbeTool'
$remoteScript = '/data/local/tmp/jesty-rp-watchdog-sentinel.sh'

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
        throw "Device sentinel action failed with exit code $LASTEXITCODE."
    }
}

function Get-OwnerIdentity {
    $pids = @((& $adb.Source shell pidof $package) -split '\s+' | Where-Object { $_ -match '^\d+$' })
    if ($pids.Count -ne 1) {
        throw "Expected exactly one running $package process; found: $($pids -join ', ')"
    }
    $pidValue = [int]$pids[0]
    $stat = ((& $adb.Source shell cat "/proc/$pidValue/stat") -join ' ').Trim()
    $close = $stat.LastIndexOf(')')
    if ($close -lt 0 -or $close + 2 -ge $stat.Length) { throw 'Could not parse owner /proc stat.' }
    $fields = $stat.Substring($close + 2).Trim() -split '\s+'
    # token 0 is Linux field 3 (state); token 19 is field 22 (starttime).
    if ($fields.Count -le 19 -or $fields[19] -notmatch '^\d+$') {
        throw 'Could not read owner process starttime.'
    }
    [PSCustomObject]@{ Pid = $pidValue; StartTicks = [Int64]$fields[19] }
}

switch ($Action) {
    'Start' {
        $identity = Get-OwnerIdentity
        $lease = [Math]::Max(5, [Math]::Min(120, $LeaseSeconds))
        Write-Host "Owner identity: pid=$($identity.Pid) start_ticks=$($identity.StartTicks)"
        Write-Host "Sentinel lease: $lease seconds"

        $temp = Join-Path ([System.IO.Path]::GetTempPath()) 'jesty-rp-watchdog-sentinel.sh'
        try {
            $scriptLines = & $adb.Source shell "CLASSPATH='$apkPath' app_process / $toolClass print-sentinel $($identity.Pid) $($identity.StartTicks) $lease"
            if ($LASTEXITCODE -ne 0 -or -not $scriptLines) {
                throw 'Could not generate sentinel script.'
            }
            [System.IO.File]::WriteAllLines($temp, [string[]]$scriptLines,
                    (New-Object System.Text.UTF8Encoding($false)))
            & $adb.Source push $temp $remoteScript | Out-Host
            if ($LASTEXITCODE -ne 0) { throw 'Could not stage sentinel script.' }
            & $adb.Source shell chmod 700 $remoteScript
            if ($LASTEXITCODE -ne 0) { throw 'Could not chmod sentinel script.' }
            Invoke-ProbeTool 'sentinel-start'
            Write-Host 'Sentinel armed. It has no charging-control command.'
        } finally {
            Remove-Item -LiteralPath $temp -Force -ErrorAction SilentlyContinue
        }
    }
    'Status' { Invoke-ProbeTool 'sentinel-status' }
    'Stop'   { Invoke-ProbeTool 'sentinel-stop' }
    'Clean'  { Invoke-ProbeTool 'sentinel-clean' }
}
