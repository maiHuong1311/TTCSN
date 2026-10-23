param([int]$Port = 8001, [string]$RuntimeDirectory = '')
$ErrorActionPreference = 'Stop'
$dataRoot = Split-Path $PSScriptRoot -Parent
$projectRoot = Split-Path $dataRoot -Parent
if (-not $RuntimeDirectory) { $RuntimeDirectory=Join-Path $dataRoot '.local/fashion-ai-venv' }
$runtime=Join-Path $RuntimeDirectory 'Scripts/python.exe'
if (-not (Test-Path -LiteralPath $runtime)) { throw 'Run project-data/scripts/Setup-AI.ps1 first.' }
$env:FORME_ENV_FILE=Join-Path $projectRoot 'backend/.env'
$env:AI_DATA_DIRECTORY=Join-Path $dataRoot '.local/ai-chroma'
$env:AI_KNOWLEDGE_DIRECTORY=Join-Path $projectRoot 'backend/src/main/resources/fashion'
$env:ANONYMIZED_TELEMETRY='False'
$env:LANGCHAIN_TRACING_V2='false'
Push-Location (Join-Path $projectRoot 'backend/ai-service')
try { & $runtime -m uvicorn forme_ai.app:create_app --factory --host 127.0.0.1 --port $Port; if ($LASTEXITCODE -ne 0) { throw 'AI service stopped with an error.' } }
finally { Pop-Location }
