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
    (Join-Path $root "src\$package\LinuxProcessIdentity.java"),
    (Join-Path $root "src\$package\RestoreWatchdogSentinelScript.java"),
    (Join-Path $root "src\$package\RestoreRetryPolicy.java"),
    (Join-Path $root "tests\$package\ProcessSurvivalProbeCommandTest.java"),
    (Join-Path $root "tests\$package\RestoreOnlyWatchdogPolicyTest.java"),
    (Join-Path $root "tests\$package\LinuxProcessIdentityTest.java"),
    (Join-Path $root "tests\$package\RestoreWatchdogSentinelScriptTest.java"),
    (Join-Path $root "tests\$package\RestoreRetryPolicyTest.java")
)

& javac -encoding UTF-8 -source 8 -target 8 -d $output @sources
if ($LASTEXITCODE -ne 0) { throw 'Process resilience research test compilation failed.' }

foreach ($test in @(
    'ProcessSurvivalProbeCommandTest',
    'RestoreOnlyWatchdogPolicyTest',
    'LinuxProcessIdentityTest',
    'RestoreWatchdogSentinelScriptTest',
    'RestoreRetryPolicyTest'
)) {
    & java -cp $output "com.jesty.rpchargingseparation.$test"
    if ($LASTEXITCODE -ne 0) { throw "$test failed." }
}
