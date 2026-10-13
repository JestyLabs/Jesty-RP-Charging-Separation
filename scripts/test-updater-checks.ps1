[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$output = Join-Path $root 'build\updater-check-tests'
New-Item -ItemType Directory -Path $output -Force | Out-Null
$src = Join-Path $root 'src\com\jesty\rpchargingseparation'
$test = Join-Path $root 'tests\com\jesty\rpchargingseparation\UpdaterCheckTest.java'
& javac.exe -encoding UTF-8 -d $output `
    (Join-Path $src 'UpdateVersion.java') `
    (Join-Path $src 'UpdateCheckPolicy.java') `
    (Join-Path $src 'UpdateHttp.java') $test
if ($LASTEXITCODE -ne 0) { throw 'Updater check test compilation failed.' }
& java.exe -cp $output com.jesty.rpchargingseparation.UpdaterCheckTest
if ($LASTEXITCODE -ne 0) { throw 'Updater check integration tests failed.' }
