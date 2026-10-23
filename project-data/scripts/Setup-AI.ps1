param([string]$Python = 'python', [string]$RuntimeDirectory = '')
$ErrorActionPreference = 'Stop'
$dataRoot = Split-Path $PSScriptRoot -Parent
$projectRoot = Split-Path $dataRoot -Parent
if (-not $RuntimeDirectory) { $RuntimeDirectory = Join-Path $dataRoot '.local/fashion-ai-venv' }
$pythonVersion = & $Python -c 'import sys; print(sys.version_info.major,sys.version_info.minor)'
if ($LASTEXITCODE -ne 0 -or $pythonVersion -notmatch '^3 (11|12|13)$') { throw 'Install Python 3.12 and pass its executable with -Python.' }
if (-not (Test-Path -LiteralPath (Join-Path $RuntimeDirectory 'Scripts/python.exe'))) { & $Python -m venv $RuntimeDirectory; if ($LASTEXITCODE -ne 0) { throw 'Could not create Python virtual environment.' } }
$runtime = Join-Path $RuntimeDirectory 'Scripts/python.exe'
$requirements = Join-Path $projectRoot 'backend/ai-service/requirements.lock.txt'
if (-not (Test-Path -LiteralPath $requirements)) { $requirements = Join-Path $projectRoot 'backend/ai-service/requirements.txt' }
& $runtime -m pip install -r $requirements
if ($LASTEXITCODE -ne 0) { throw 'Could not install AI dependencies.' }
$envFile = Join-Path $projectRoot 'backend/.env'
$lines = if (Test-Path -LiteralPath $envFile) { [IO.File]::ReadAllLines($envFile) } else { @() }
$values = @{}
foreach ($line in $lines) { if ($line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') { $values[$matches[1]]=$matches[2] } }
$token=$values['AI_API_KEY']
if (-not $token -or $token.Length -lt 24) {
    $bytes=New-Object byte[] 32
    $rng=[Security.Cryptography.RandomNumberGenerator]::Create(); $rng.GetBytes($bytes); $rng.Dispose()
    $token=([BitConverter]::ToString($bytes)).Replace('-','').ToLowerInvariant()
}
$updates=@{AI_API_URL='http://127.0.0.1:8001';AI_API_KEY=$token;AI_CONNECT_TIMEOUT_SECONDS='2';AI_READ_TIMEOUT_SECONDS='30'}
$defaults=@{GEMINI_API_KEY='';GEMINI_MODEL='gemini-3.5-flash-lite';GEMINI_EMBEDDING_MODEL='gemini-embedding-001';AI_EMBEDDING_MODE='gemini'}
foreach ($entry in $defaults.GetEnumerator()) { if (-not $values.ContainsKey($entry.Key)) { $updates[$entry.Key]=$entry.Value } }
$output = [Collections.Generic.List[string]]::new()
foreach ($line in $lines) {
    if ($line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$' -and $updates.ContainsKey($matches[1])) { continue }
    $output.Add($line)
}
foreach ($key in ($updates.Keys | Sort-Object)) { $output.Add($key+'='+$updates[$key]) }
[IO.File]::WriteAllLines($envFile,$output,[Text.UTF8Encoding]::new($false))
Write-Output 'AI local is ready. Set GEMINI_API_KEY in backend/.env yourself; do not put a Google key in AI_API_KEY.'
Write-Output ('Runtime: '+$runtime)
