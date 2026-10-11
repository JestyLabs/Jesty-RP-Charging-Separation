[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$output = Join-Path $root 'build\quick-settings-tests'
New-Item -ItemType Directory -Path $output -Force | Out-Null
$package = 'com\jesty\rpchargingseparation'
$sources = @('SeparationControl', 'ChargingSeparationTileService', 'TileStatus',
    'BypassController', 'ChargeLimitPolicy', 'PowerTelemetry') | ForEach-Object {
        Join-Path $root "src\$package\$_.java"
    }
$sources += Join-Path $root "tests\$package\QuickSettingsTileTest.java"
$sources += Get-ChildItem (Join-Path $root 'tests\fixtures\quick-settings') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
& javac -encoding UTF-8 -source 8 -target 8 -d $output @sources
if ($LASTEXITCODE -ne 0) { throw 'Quick Settings fixture compilation failed.' }
& java -cp $output com.jesty.rpchargingseparation.QuickSettingsTileTest
if ($LASTEXITCODE -ne 0) { throw 'Quick Settings regressions failed.' }
[xml]$manifest = Get-Content (Join-Path $root 'AndroidManifest.xml') -Raw
$ns = [Xml.XmlNamespaceManager]::new($manifest.NameTable)
$ns.AddNamespace('a', 'http://schemas.android.com/apk/res/android')
$tile = $manifest.SelectSingleNode('//service[@a:name=".ChargingSeparationTileService"]', $ns)
if (-not $tile -or $tile.GetAttribute('permission', $ns.LookupNamespace('a')) -ne 'android.permission.BIND_QUICK_SETTINGS_TILE' -or
    $tile.GetAttribute('exported', $ns.LookupNamespace('a')) -ne 'true' -or
    -not $tile.SelectSingleNode('intent-filter/action[@a:name="android.service.quicksettings.action.QS_TILE"]', $ns)) {
    throw 'Tile must be discoverable and protected by the Android tile binding permission.'
}
Write-Host 'Quick Settings manifest binding guard passed.'
