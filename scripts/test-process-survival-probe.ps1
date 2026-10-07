[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$output = Join-Path $root 'build\process-survival-probe-tests'
New-Item -ItemType Directory -Path $output -Force | Out-Null

$package = 'com\jesty\rpchargingseparation'
$sources = @(
    (Join-Path $root "src\$package\ProcessSurvivalProbeCommand.java"),
    (Join-Path $root "src\$package\RestoreOnlyWatchdogPolicy.java"),
    (Join-Path $root "tests\$package\ProcessSurvivalProbeCommandTest.java"),
    (Join-Path $root "tests\$package\RestoreOnlyWatchdogPolicyTest.java")
)

& javac -encoding UTF-8 -source 8 -target 8 -d $output @sources
if ($LASTEXITCODE -ne 0) { throw 'Process resilience research test compilation failed.' }

& java -cp $output com.jesty.rpchargingseparation.ProcessSurvivalProbeCommandTest
if ($LASTEXITCODE -ne 0) { throw 'Process survival probe tests failed.' }

& java -cp $output com.jesty.rpchargingseparation.RestoreOnlyWatchdogPolicyTest
if ($LASTEXITCODE -ne 0) { throw 'Restore-only watchdog policy tests failed.' }
