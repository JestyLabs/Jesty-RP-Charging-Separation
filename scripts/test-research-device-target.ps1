[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$global:JestyRpFixtureCalls = [Collections.Generic.List[object]]::new()
$global:JestyRpFixtureState = 'device'
$global:JestyRpFixtureModel = 'Retroid Pocket Flip2'
$global:JestyRpFixtureArtifact = $true
$global:JestyRpFixtureStaged = ''
$global:JestyRpPrinterClasses = Join-Path (Split-Path -Parent $PSScriptRoot) 'build\process-survival-probe-tests'
function Fixture-RpAdb {
    $global:JestyRpFixtureCalls.Add(@($args))
    $global:LASTEXITCODE = 0
    $text = $args -join ' '
    if ($text -match 'get-state$') { return $global:JestyRpFixtureState }
    if ($text -match 'getprop ro.product.model$') { return $global:JestyRpFixtureModel }
    if ($text -match 'test -r') {
        if (-not $global:JestyRpFixtureArtifact) { $global:LASTEXITCODE = 1 }
        return
    }
    if ($text -match 'print-launcher (\d+) ''([^'']+)''') {
        return & java -cp $global:JestyRpPrinterClasses com.jesty.rpchargingseparation.ProcessSurvivalProbeTool print-launcher $Matches[1] $Matches[2]
    }
    if ($text -match 'print-sentinel (\d+) (\d+) (\d+) ''([^'']+)''') {
        return & java -cp $global:JestyRpPrinterClasses com.jesty.rpchargingseparation.ProcessSurvivalProbeTool print-sentinel $Matches[1] $Matches[2] $Matches[3] $Matches[4]
    }
    if ($args[2] -eq 'push') {
        $global:JestyRpFixtureStaged = Get-Content -LiteralPath $args[3] -Raw
        return
    }
    if ($text -match 'pidof') { return '123' }
    if ($text -match '/proc/123/stat') { return '123 (fixture owner) ' + ((@('S') + (1..18 | ForEach-Object { '0' }) + @('456')) -join ' ') }
    return 'fixture result'
}
function Expect-Rejected([scriptblock]$Action) {
    $rejected = $false
    try { & $Action | Out-Null } catch { $rejected = $true }
    if (-not $rejected) { throw 'Unsafe device selection was accepted.' }
}
foreach ($name in @('device-process-survival-probe.ps1','device-watchdog-sentinel.ps1')) {
    $tool = Join-Path $PSScriptRoot $name
    $source = Get-Content -LiteralPath $tool -Raw
    if ($source -match '&\s+\$AdbPath\s+(?!-s\s+\$Serial\b)') { throw 'Unpinned ADB call in research tool.' }
    $global:JestyRpFixtureCalls.Clear()
    Expect-Rejected { & $tool -Serial '' -ResearchApk /data/local/tmp/research.apk -AdbPath Fixture-RpAdb }
    Expect-Rejected { & $tool -Serial fixture-flip2 -ResearchApk /data/app/stable.apk -AdbPath Fixture-RpAdb }
    Expect-Rejected { & $tool -Serial fixture-flip2 -ResearchApk /data/local/tmp/../stable.apk -AdbPath Fixture-RpAdb }
    if ($global:JestyRpFixtureCalls.Count) { throw 'Invalid inputs contacted ADB.' }
    $global:JestyRpFixtureState = 'offline'
    Expect-Rejected { & $tool -Serial fixture-flip2 -ResearchApk /data/local/tmp/research.apk -AdbPath Fixture-RpAdb }
    if ($global:JestyRpFixtureCalls.Count -ne 1) { throw 'Offline target received further commands.' }
    $global:JestyRpFixtureState = 'device'
    $global:JestyRpFixtureModel = 'Thor'
    $global:JestyRpFixtureCalls.Clear()
    Expect-Rejected { & $tool -Serial fixture-flip2 -ResearchApk /data/local/tmp/research.apk -AdbPath Fixture-RpAdb }
    if ($global:JestyRpFixtureCalls.Count -ne 2) { throw 'Wrong model received research commands.' }
    $global:JestyRpFixtureModel = 'Retroid Pocket Flip2'
    $global:JestyRpFixtureArtifact = $false
    $global:JestyRpFixtureCalls.Clear()
    Expect-Rejected { & $tool -Serial fixture-flip2 -ResearchApk /data/local/tmp/research.apk -AdbPath Fixture-RpAdb }
    if ($global:JestyRpFixtureCalls.Count -ne 3) { throw 'Unavailable artifact was executed.' }
    $global:JestyRpFixtureArtifact = $true
    $global:JestyRpFixtureCalls.Clear()
    & $tool -Serial fixture-flip2 -ResearchApk /data/local/tmp/research.apk -AdbPath Fixture-RpAdb -Action Status | Out-Null
    if ($global:JestyRpFixtureCalls.Count -ne 4) { throw 'Expected validation before Status.' }
    $global:JestyRpFixtureStaged = ''
    & $tool -Serial fixture-flip2 -ResearchApk /data/local/tmp/session/candidate.apk -AdbPath Fixture-RpAdb -Action Start | Out-Null
    if ($global:JestyRpFixtureStaged -notmatch "export CLASSPATH='/data/local/tmp/session/candidate.apk'" -or
        $global:JestyRpFixtureStaged -match 'pm path') { throw 'Harness/printer/worker lost the selected candidate.' }
    foreach ($call in $global:JestyRpFixtureCalls) {
        if ($call[0] -ne '-s' -or $call[1] -ne 'fixture-flip2') { throw 'Unpinned invocation.' }
    }
}
Write-Host 'Research device-target fixtures passed without device calls.'

# Real tool subprocesses must reject missing/negative transport receipts.
try {
    foreach ($receipt in @('unavailable', 'BUSY', 'OK')) {
        $env:JESTY_TEST_RECEIPT = $receipt
        $result = & java -cp $global:JestyRpPrinterClasses com.jesty.rpchargingseparation.ProcessSurvivalProbeTool clean 2>&1
        if (($receipt -eq 'OK') -ne ($LASTEXITCODE -eq 0)) { throw 'Tool accepted a missing/negative acknowledgement.' }
    }
} finally { Remove-Item Env:JESTY_TEST_RECEIPT -ErrorAction SilentlyContinue }
Write-Host 'Research action acknowledgement regressions passed.'
