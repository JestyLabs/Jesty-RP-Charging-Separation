[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[A-Za-z0-9._:-]+$')][string]$Serial,
    [Parameter(Mandatory=$true)][ValidatePattern('^/data/local/tmp/(?:[A-Za-z0-9_-][A-Za-z0-9._-]*/)*[A-Za-z0-9_-][A-Za-z0-9._-]*\.apk$')][string]$ResearchApk,
    [string]$AdbPath = 'adb.exe',
    [ValidateSet('Start', 'Status', 'Stop', 'Clean')]
    [string]$Action = 'Status',
    [int]$DurationSeconds = 600
)

$ErrorActionPreference = 'Stop'
$package = 'com.jesty.rpchargingseparation'
$toolClass = 'com.jesty.rpchargingseparation.ProcessSurvivalProbeTool'
$remoteLauncher = '/data/local/tmp/jesty-rp-process-survival-launch.sh'

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
        throw "Device probe action failed with exit code $LASTEXITCODE."
    }
}

switch ($Action) {
    'Start' {
        $duration = [Math]::Max(60, [Math]::Min(1800, $DurationSeconds))
        $temp = [System.IO.Path]::GetTempFileName()
        try {
            $scriptLines = & $AdbPath -s $Serial shell "CLASSPATH='$apkPath' app_process / $toolClass print-launcher $duration '$apkPath'"
            if ($LASTEXITCODE -ne 0 -or -not $scriptLines) {
                throw 'Could not generate the device-side launcher script.'
            }
            [System.IO.File]::WriteAllText($temp, (($scriptLines -join "`n") + "`n"),
                    (New-Object System.Text.UTF8Encoding($false)))
            & $AdbPath -s $Serial push $temp $remoteLauncher | Out-Host
            if ($LASTEXITCODE -ne 0) { throw 'Could not stage launcher script.' }
            & $AdbPath -s $Serial shell chmod 700 $remoteLauncher
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
