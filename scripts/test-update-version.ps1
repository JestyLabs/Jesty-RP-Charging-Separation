[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$output = Join-Path $root 'build\update-version-tests'
New-Item -ItemType Directory -Path $output -Force | Out-Null

$source = Join-Path $root 'src\com\jesty\rpchargingseparation\UpdateVersion.java'
$test = Join-Path $root 'tests\com\jesty\rpchargingseparation\UpdateVersionTest.java'
& javac.exe -encoding UTF-8 -d $output $source $test
if ($LASTEXITCODE -ne 0) { throw 'Update-version test compilation failed.' }
& java.exe -cp $output com.jesty.rpchargingseparation.UpdateVersionTest
if ($LASTEXITCODE -ne 0) { throw 'Update-version tests failed.' }
