param([switch]$Lan)
$ErrorActionPreference = 'Stop'
$dataRoot = Split-Path $PSScriptRoot -Parent
$projectRoot = Split-Path $dataRoot -Parent
Push-Location (Join-Path $projectRoot 'frontend')
try {
    if (-not (Test-Path -LiteralPath 'node_modules')) {
        npm ci --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
    }
    if ($Lan) { npm run dev -- --host 0.0.0.0 } else { npm run dev }
} finally { Pop-Location }
