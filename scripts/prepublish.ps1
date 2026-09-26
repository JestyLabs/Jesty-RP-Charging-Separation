[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot

$stagedFiles = @(git -C $repository diff --cached --name-only --diff-filter=ACMR)
if ($LASTEXITCODE -ne 0) { throw 'Unable to enumerate staged files.' }
if ($stagedFiles.Count -eq 0) { throw 'Nothing is staged for review.' }

$forbiddenExtensions = '\.(apk|aab|apks|idsig|jks|keystore|p12|pem|key|dmp|log)$'
$forbiddenFiles = @($stagedFiles | Where-Object { $_ -match $forbiddenExtensions })
if ($forbiddenFiles.Count -gt 0) {
    throw "Forbidden release artifacts are staged:`n$($forbiddenFiles -join "`n")"
}

$privacyPattern = 'C:\\Users\\|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|gh[pousr]_[A-Za-z0-9_]+'
if ($env:JESTY_PRIVATE_TERMS) {
    $privatePattern = (($env:JESTY_PRIVATE_TERMS -split ';' |
        Where-Object { $_ } |
        ForEach-Object { [Regex]::Escape($_) }) -join '|')
    if ($privatePattern) { $privacyPattern = "($privacyPattern)|($privatePattern)" }
}
$matches = @(git -C $repository grep --cached -n -I -E $privacyPattern -- `
    . ':(exclude)LICENSE' ':(exclude)scripts/prepublish.ps1')
if ($LASTEXITCODE -notin 0, 1) { throw 'The staged privacy scan failed.' }
if ($matches.Count -gt 0) {
    throw "Potential personal data or secret found:`n$($matches -join "`n")"
}

Write-Host "Pre-publication scan passed for $($stagedFiles.Count) staged files."
