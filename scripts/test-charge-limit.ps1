[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$output = Join-Path $root 'build\charge-limit-tests'
New-Item -ItemType Directory -Path $output -Force | Out-Null

$source = Join-Path $root 'src\com\jesty\rpchargingseparation\ChargeLimitPolicy.java'
$test = Join-Path $root 'tests\com\jesty\rpchargingseparation\ChargeLimitPolicyTest.java'
& javac.exe -encoding UTF-8 -d $output $source $test
if ($LASTEXITCODE -ne 0) { throw 'Charge-limit test compilation failed.' }
& java.exe -cp $output com.jesty.rpchargingseparation.ChargeLimitPolicyTest
if ($LASTEXITCODE -ne 0) { throw 'Charge-limit tests failed.' }
