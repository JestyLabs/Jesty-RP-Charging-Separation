[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$output = Join-Path $root 'build\temperature-extremes-tests'
New-Item -ItemType Directory -Path $output -Force | Out-Null
$src = Join-Path $root 'src\com\jesty\rpchargingseparation'
$tests = Join-Path $root 'tests\com\jesty\rpchargingseparation'
$fixture = Join-Path $root 'tests\fixtures\temperature\android\content\SharedPreferences.java'
& javac.exe -encoding UTF-8 -d $output `
    $fixture `
    (Join-Path $src 'BatteryTemperatureExtremes.java') `
    (Join-Path $src 'BatteryTemperatureHistory.java') `
    (Join-Path $tests 'BatteryTemperatureExtremesTest.java') `
    (Join-Path $tests 'BatteryTemperatureHistoryTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Battery temperature test compilation failed.' }
& java.exe -cp $output com.jesty.rpchargingseparation.BatteryTemperatureExtremesTest
if ($LASTEXITCODE -ne 0) { throw 'Battery extrema tests failed.' }
& java.exe -cp $output com.jesty.rpchargingseparation.BatteryTemperatureHistoryTest
if ($LASTEXITCODE -ne 0) { throw 'Battery persistence tests failed.' }
