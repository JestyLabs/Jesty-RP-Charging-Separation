[CmdletBinding()]
param(
    [string]$AndroidSdk,
    [string]$JdkHome,
    [string]$Keystore,
    [string]$KeyAlias,
    [string]$OutputName = 'Jesty-RP-Charging-Separation-1.5.3'
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$buildRoot = Join-Path $projectRoot 'build'
$distRoot = Join-Path $projectRoot 'dist'

if (-not $AndroidSdk) {
    $AndroidSdk = $env:ANDROID_SDK_ROOT
}
if (-not $AndroidSdk) {
    $AndroidSdk = $env:ANDROID_HOME
}
if (-not $AndroidSdk) {
    $localAppData = [Environment]::GetFolderPath('LocalApplicationData')
    $candidate = Join-Path $localAppData 'Android\Sdk'
    if (Test-Path -LiteralPath $candidate) {
        $AndroidSdk = $candidate
    }
}
if (-not $AndroidSdk -or -not (Test-Path -LiteralPath $AndroidSdk)) {
    throw 'Android SDK not found. Pass -AndroidSdk or set ANDROID_SDK_ROOT.'
}

$buildTools = Get-ChildItem -LiteralPath (Join-Path $AndroidSdk 'build-tools') -Directory |
    Sort-Object { [version]$_.Name } -Descending |
    Select-Object -First 1
if (-not $buildTools) {
    throw 'Android SDK Build Tools were not found.'
}

$platform = Get-ChildItem -LiteralPath (Join-Path $AndroidSdk 'platforms') -Directory |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'android.jar') } |
    Sort-Object { [int]($_.Name -replace '^android-', '') } -Descending |
    Select-Object -First 1
if (-not $platform) {
    throw 'No Android platform containing android.jar was found.'
}

$aapt2 = Join-Path $buildTools.FullName 'aapt2.exe'
$aapt = Join-Path $buildTools.FullName 'aapt.exe'
$d8 = Join-Path $buildTools.FullName 'd8.bat'
$zipalign = Join-Path $buildTools.FullName 'zipalign.exe'
$apksigner = Join-Path $buildTools.FullName 'apksigner.bat'
$androidJar = Join-Path $platform.FullName 'android.jar'

if ($JdkHome) {
    $javac = Join-Path $JdkHome 'bin\javac.exe'
} else {
    $javacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    if (-not $javacCommand) {
        throw 'javac was not found. Pass -JdkHome or add the JDK to PATH.'
    }
    $javac = $javacCommand.Source
}

$requiredTools = @($aapt2, $aapt, $d8, $zipalign, $javac)
foreach ($tool in $requiredTools) {
    if (-not (Test-Path -LiteralPath $tool)) {
        throw "Required build tool not found: $tool"
    }
}

if (Test-Path -LiteralPath $buildRoot) {
    $resolvedBuild = (Resolve-Path -LiteralPath $buildRoot).Path
    if (-not $resolvedBuild.StartsWith($projectRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to clean outside the project: $resolvedBuild"
    }
    Remove-Item -LiteralPath $resolvedBuild -Recurse -Force
}
New-Item -ItemType Directory -Path $buildRoot, $distRoot -Force | Out-Null

$compiled = Join-Path $buildRoot 'compiled'
$generated = Join-Path $buildRoot 'generated'
$classes = Join-Path $buildRoot 'classes'
$dex = Join-Path $buildRoot 'dex'
New-Item -ItemType Directory -Path $compiled, $generated, $classes, $dex -Force | Out-Null

& $aapt2 compile --dir (Join-Path $projectRoot 'res') -o $compiled
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed.' }

$resourceApk = Join-Path $buildRoot 'resources.ap_'
$flatFiles = Get-ChildItem -LiteralPath $compiled -Filter '*.flat' -File |
    Select-Object -ExpandProperty FullName
& $aapt2 link -o $resourceApk -I $androidJar --manifest (Join-Path $projectRoot 'AndroidManifest.xml') `
    --java $generated --min-sdk-version 28 --target-sdk-version 28 `
    --version-code 22 --version-name '1.5.3' $flatFiles
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed.' }

$sourceFiles = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Recurse -Filter '*.java' -File |
    Select-Object -ExpandProperty FullName
$generatedFiles = Get-ChildItem -LiteralPath $generated -Recurse -Filter '*.java' -File |
    Select-Object -ExpandProperty FullName
& $javac -encoding UTF-8 -source 8 -target 8 -classpath $androidJar -d $classes `
    $sourceFiles $generatedFiles
if ($LASTEXITCODE -ne 0) { throw 'javac failed.' }

$classFiles = Get-ChildItem -LiteralPath $classes -Recurse -Filter '*.class' -File |
    Select-Object -ExpandProperty FullName
& $d8 --min-api 28 --lib $androidJar --output $dex $classFiles
if ($LASTEXITCODE -ne 0) { throw 'd8 failed.' }

$unsigned = Join-Path $buildRoot "$OutputName-unsigned.apk"
Copy-Item -LiteralPath $resourceApk -Destination $unsigned -Force
Push-Location $dex
try {
    & $aapt add $unsigned 'classes.dex'
    if ($LASTEXITCODE -ne 0) { throw 'Adding classes.dex failed.' }
} finally {
    Pop-Location
}

$aligned = Join-Path $distRoot "$OutputName-unsigned.apk"
& $zipalign -f -p 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed.' }

if ($Keystore -or $KeyAlias) {
    if (-not $Keystore -or -not $KeyAlias) {
        throw 'Signing requires both -Keystore and -KeyAlias.'
    }
    if (-not (Test-Path -LiteralPath $Keystore)) {
        throw "Keystore not found: $Keystore"
    }
    if (-not (Test-Path -LiteralPath $apksigner)) {
        throw "apksigner not found: $apksigner"
    }
    $signed = Join-Path $distRoot "$OutputName.apk"
    $securePassword = Read-Host 'Keystore password' -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
        $passwordInput = "$plainPassword`n$plainPassword"
        $passwordInput | & $apksigner sign --ks $Keystore --ks-key-alias $KeyAlias `
            --ks-pass stdin --key-pass stdin --out $signed $aligned
        if ($LASTEXITCODE -ne 0) { throw 'APK signing failed.' }
        & $apksigner verify --verbose --print-certs $signed
        if ($LASTEXITCODE -ne 0) { throw 'Signed APK verification failed.' }
        Write-Host "Signed APK: $signed"
    } finally {
        if ($passwordPointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
        }
        $plainPassword = $null
        $passwordInput = $null
    }
} else {
    Write-Host "Unsigned APK: $aligned"
}

Get-FileHash -Algorithm SHA256 -LiteralPath $aligned | Format-List
