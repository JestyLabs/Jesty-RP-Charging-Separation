[CmdletBinding()]
param()

# Fail closed on unsafe tracked text and PR metadata. Never print matched values.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

$files = @(git -C $root ls-files)
if ($LASTEXITCODE -ne 0) { throw 'Unable to enumerate tracked repository files.' }

$forbiddenSuffix = '(?i)\.(?:apk|aab|apks|idsig|jks|keystore|p12|pfx|pem|key|dmp|log|logcat|trace|bugreport|pcap)$'
$textExtensions = @('.md', '.txt', '.java', '.ps1', '.psm1', '.py', '.sh',
    '.yml', '.yaml', '.json', '.xml', '.toml', '.properties', '.gradle',
    '.kts', '.kt', '.csv')

# Keep project-specific exclusions out of both source and CI log output.
$a = -join ([char[]]@(83,108,101,101,112))
$b = -join ([char[]]@(77,97,110,97,103,101,114))
$c = -join ([char[]]@(87,97,121,102,105,110,100,101,114))

$checks = @(
    @{ Label = 'excluded external project'; Regex = '(?i)' + [regex]::Escape($a) + '[\s_-]*' + [regex]::Escape($b) },
    @{ Label = 'excluded external project'; Regex = '(?i)' + [regex]::Escape($c) },
    @{ Label = 'personal Windows home path'; Regex = '(?i)[A-Z]:[\\/](?:Users|Documents and Settings)[\\/][^\s\\/]+[\\/]' },
    @{ Label = 'personal POSIX home path'; Regex = '(?i)/(?:home|Users)/[a-z_][a-z0-9._-]+/' },
    @{ Label = 'email address'; Regex = '(?i)\b[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}\b' },
    @{ Label = 'GitHub access token'; Regex = '\b(?:gh[pousr]_[a-zA-Z0-9_]{20,}|github_pat_[a-zA-Z0-9_]{20,})\b' },
    @{ Label = 'private key block'; Regex = '-----BEGIN\s+(?:(?:RSA|EC|OPENSSH|DSA)\s+)?PRIVATE\s+KEY-----' }
)

if ($env:JESTY_PRIVATE_TERMS) {
    foreach ($term in ($env:JESTY_PRIVATE_TERMS -split ';')) {
        if (-not [string]::IsNullOrWhiteSpace($term)) {
            $checks += @{ Label = 'local private term'; Regex = '(?i)' + [regex]::Escape($term.Trim()) }
        }
    }
}

$problems = [System.Collections.Generic.List[string]]::new()
foreach ($path in $files) {
    if ($path -match $forbiddenSuffix) {
        $problems.Add("Forbidden tracked artifact: $path")
        continue
    }

    if ([IO.Path]::GetExtension($path).ToLowerInvariant() -notin $textExtensions) { continue }
    if ($path -eq 'LICENSE') { continue }
    $fullPath = Join-Path $root $path
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) { continue }

    $lines = [IO.File]::ReadAllLines($fullPath)
    for ($i = 0; $i -lt $lines.Length; $i++) {
        foreach ($check in $checks) {
            if ([regex]::IsMatch($lines[$i], [string]$check.Regex)) {
                $problems.Add(('{0}:{1}: {2}' -f $path, ($i + 1), $check.Label))
            }
        }
    }
}

if ($env:GITHUB_EVENT_PATH -and (Test-Path -LiteralPath $env:GITHUB_EVENT_PATH)) {
    $event = Get-Content -LiteralPath $env:GITHUB_EVENT_PATH -Raw | ConvertFrom-Json
    if ($null -ne $event.pull_request) {
        foreach ($field in @('title', 'body')) {
            $value = [string]($event.pull_request.$field)
            foreach ($check in $checks) {
                if ([regex]::IsMatch($value, [string]$check.Regex)) {
                    $problems.Add(('Pull request {0}: {1}' -f $field, $check.Label))
                }
            }
        }
    }
}

if ($problems.Count -gt 0) {
    throw ('Public content policy failed (matches redacted):' + [Environment]::NewLine + ($problems -join [Environment]::NewLine))
}

Write-Host "Public content policy passed ($($files.Count) tracked paths checked; images require manual review)."
