[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$output = Join-Path $root 'build\temperature-extremes-tests'
New-Item -ItemType Directory -Path $output -Force | Out-Null
$source = Join-Path $root 'src\com\jesty\rpchargingseparation\BatteryTemperatureExtremes.java'
$test = Join-Path $root 'tests\com\jesty\rpchargingseparation\BatteryTemperatureExtremesTest.java'
& javac.exe -encoding UTF-8 -d $output $source $test
if ($LASTEXITCODE -ne 0) { throw 'Battery temperature test compilation failed.' }
& java.exe -cp $output com.jesty.rpchargingseparation.BatteryTemperatureExtremesTest
if ($LASTEXITCODE -ne 0) { throw 'Battery temperature tests failed.' }
