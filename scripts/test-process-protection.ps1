[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$output = Join-Path $root 'build\process-protection-tests'
New-Item -ItemType Directory -Path $output -Force | Out-Null

$package = 'com\jesty\rpchargingseparation'
$sources = @(
    (Join-Path $root "src\$package\RetroidProcessProtection.java"),
    (Join-Path $root "tests\$package\RetroidProcessProtectionTest.java")
)
& javac -encoding UTF-8 -d $output @sources
if ($LASTEXITCODE -ne 0) { throw 'Process-protection test compilation failed.' }
& java -cp $output com.jesty.rpchargingseparation.RetroidProcessProtectionTest
if ($LASTEXITCODE -ne 0) { throw 'Process-protection tests failed.' }
