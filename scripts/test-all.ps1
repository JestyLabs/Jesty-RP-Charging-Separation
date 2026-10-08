[CmdletBinding()]
param()
# Runs every host/static test without a device or Android SDK.
$ErrorActionPreference = 'Stop'
foreach ($script in @(
    'test-charge-limit.ps1',
    'test-update-version.ps1',
    'test-bypass-controller.ps1',
    'test-process-protection.ps1',
    'test-process-protection-capture.ps1',
    'test-process-survival-probe.ps1',
    'test-process-resilience-safety.ps1',
    'test-research-device-target.ps1'
)) {
    Write-Host "== $script"
    & (Join-Path $PSScriptRoot $script)
}
Write-Host 'All tests passed.'
