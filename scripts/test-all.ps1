[CmdletBinding()]
param()

# Runs every JDK-only test. No device, root access or Android SDK is required.
$ErrorActionPreference = 'Stop'
foreach ($script in @('test-charge-limit.ps1', 'test-update-version.ps1', 'test-bypass-controller.ps1')) {
    Write-Host "== $script"
    & (Join-Path $PSScriptRoot $script)
}
Write-Host 'All tests passed.'
