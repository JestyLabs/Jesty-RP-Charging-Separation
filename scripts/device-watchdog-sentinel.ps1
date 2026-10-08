[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[A-Za-z0-9._:-]+$')][string]$Serial,
    [Parameter(Mandatory=$true)][ValidatePattern('^/data/local/tmp/(?:[A-Za-z0-9_-][A-Za-z0-9._-]*/)*[A-Za-z0-9_-][A-Za-z0-9._-]*\.apk$')][string]$ResearchApk,
    [string]$AdbPath = 'adb.exe',
    [ValidateSet('Start', 'Status', 'Stop', 'Clean')]
    [string]$Action = 'Status',
    [int]$LeaseSeconds = 30
)

$ErrorActionPreference = 'Stop'
$package = 'com.jesty.rpchargingseparation'
$toolClass = 'com.jesty.rpchargingseparation.ProcessSurvivalProbeTool'
$remoteScript = '/data/local/tmp/jesty-rp-watchdog-sentinel.sh'

Get-Command $AdbPath -ErrorAction Stop | Out-Null
$state = (& $AdbPath -s $Serial get-state 2>$null | Select-Object -First 1)
if ($LASTEXITCODE -ne 0 -or "$state".Trim() -ne 'device') { throw 'Selected device is not ready.' }
$model = (& $AdbPath -s $Serial shell getprop ro.product.model) -join ' '
if ($LASTEXITCODE -ne 0 -or $model -notmatch '(?i)flip\s*2') { throw 'Selected device is not a Flip 2.' }
$apkPath = $ResearchApk
& $AdbPath -s $Serial shell "test -r '$apkPath'"
if ($LASTEXITCODE -ne 0) { throw 'Stage the research APK separately; do not replace the stable app.' }

function Invoke-ProbeTool([string]$Arguments) {
    $remote = "CLASSPATH='$apkPath' app_process / $toolClass $Arguments"
    & $AdbPath -s $Serial shell $remote
    if ($LASTEXITCODE -ne 0) {
        throw "Device sentinel action failed with exit code $LASTEXITCODE."
    }
}

function Get-OwnerIdentity {
    $pids = @((& $AdbPath -s $Serial shell pidof $package) -split '\s+' | Where-Object { $_ -match '^\d+$' })
    if ($LASTEXITCODE -ne 0) { throw 'Owner process lookup unavailable.' }
    if ($pids.Count -ne 1) {
        throw "Expected exactly one running $package process; found: $($pids -join ', ')"
    }
    $pidValue = [int]$pids[0]
    $stat = ((& $AdbPath -s $Serial shell cat "/proc/$pidValue/stat") -join ' ').Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Owner starttime read unavailable.' }
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

        $temp = [System.IO.Path]::GetTempFileName()
        try {
            $scriptLines = & $AdbPath -s $Serial shell "CLASSPATH='$apkPath' app_process / $toolClass print-sentinel $($identity.Pid) $($identity.StartTicks) $lease '$apkPath'"
            if ($LASTEXITCODE -ne 0 -or -not $scriptLines) {
                throw 'Could not generate sentinel script.'
            }
            [System.IO.File]::WriteAllText($temp, (($scriptLines -join "`n") + "`n"),
                    (New-Object System.Text.UTF8Encoding($false)))
            & $AdbPath -s $Serial push $temp $remoteScript | Out-Host
            if ($LASTEXITCODE -ne 0) { throw 'Could not stage sentinel script.' }
            & $AdbPath -s $Serial shell chmod 700 $remoteScript
            if ($LASTEXITCODE -ne 0) { throw 'Could not chmod sentinel script.' }
            Invoke-ProbeTool 'sentinel-start'
            Write-Host 'Sentinel launch acknowledged; verify worker identity with Status.'
        } finally {
            Remove-Item -LiteralPath $temp -Force -ErrorAction SilentlyContinue
        }
    }
    'Status' { Invoke-ProbeTool 'sentinel-status' }
    'Stop'   { Invoke-ProbeTool 'sentinel-stop' }
    'Clean'  { Invoke-ProbeTool 'sentinel-clean' }
}
