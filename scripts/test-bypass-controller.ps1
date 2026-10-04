[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$output = Join-Path $root 'build\bypass-controller-tests'
New-Item -ItemType Directory -Path $output -Force | Out-Null

$package = 'com\jesty\rpchargingseparation'
$sources = @(
    (Join-Path $root "src\$package\BypassController.java"),
    (Join-Path $root "src\$package\ChargeLimitPolicy.java"),
    (Join-Path $root "src\$package\PowerTelemetry.java"),
    (Join-Path $root "src\$package\EventLog.java"),
    (Join-Path $root "tests\$package\BypassControllerTest.java"),
    (Join-Path $root "tests\$package\EventLogTest.java")
)
& javac -encoding UTF-8 -d $output @sources
if ($LASTEXITCODE -ne 0) { throw 'Bypass controller test compilation failed.' }
& java -cp $output com.jesty.rpchargingseparation.BypassControllerTest
if ($LASTEXITCODE -ne 0) { throw 'Bypass controller tests failed.' }
& java -cp $output com.jesty.rpchargingseparation.EventLogTest
if ($LASTEXITCODE -ne 0) { throw 'Event log tests failed.' }
