param([ValidateRange(1024,65535)][int]$Port = 6379)
$ErrorActionPreference = 'Stop'
$dataRoot = Split-Path $PSScriptRoot -Parent
$projectRoot = Split-Path $dataRoot -Parent
$localRoot = Join-Path $dataRoot '.local'
New-Item -ItemType Directory -Force -Path $localRoot | Out-Null

function Test-RedisPing {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connection = $client.ConnectAsync('127.0.0.1', $Port)
        if (-not $connection.Wait(500)) { return $false }
        $stream = $client.GetStream()
        $stream.ReadTimeout = 1000
        $stream.WriteTimeout = 1000
        $request = [System.Text.Encoding]::ASCII.GetBytes("*1`r`n`$4`r`nPING`r`n")
        $stream.Write($request, 0, $request.Length)
        $buffer = New-Object byte[] 128
        $length = $stream.Read($buffer, 0, $buffer.Length)
        return [System.Text.Encoding]::ASCII.GetString($buffer, 0, $length).StartsWith('+PONG')
    } catch { return $false } finally { $client.Dispose() }
}
if (Test-RedisPing) { Write-Output "Redis is already running at 127.0.0.1:$Port"; return }

$version = '8.10.2'
$binaryRoot = Join-Path $localRoot "redis-$version"
$archive = Join-Path $localRoot "redis-$version.zip"
$expectedHash = '6de5cc7f5adbf97b5928b13766383d4ad424626ef3d8b313ffff12d820ec6fc1'
$server = Get-ChildItem -LiteralPath $binaryRoot -Filter redis-server.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $server) {
    $url = "https://github.com/redis-windows/redis-windows/releases/download/$version/Redis-$version-Windows-x64-cygwin.zip"
    if (-not (Test-Path -LiteralPath $archive)) { Invoke-WebRequest -Uri $url -OutFile $archive }
    $actualHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualHash -ne $expectedHash) { throw 'Redis archive checksum does not match the pinned release.' }
    Expand-Archive -LiteralPath $archive -DestinationPath $binaryRoot -Force
    $server = Get-ChildItem -LiteralPath $binaryRoot -Filter redis-server.exe -Recurse | Select-Object -First 1
    if (-not $server) { throw 'Redis executable was not found in the verified archive.' }
}
function ConvertTo-CygwinPath([string]$Path) {
    $full = [System.IO.Path]::GetFullPath($Path)
    return '/cygdrive/' + $full.Substring(0,1).ToLowerInvariant() + $full.Substring(2).Replace('\','/')
}
$dataDirectory = Join-Path $localRoot 'redis-data'
New-Item -ItemType Directory -Force -Path $dataDirectory | Out-Null
$config = Join-Path $localRoot 'redis-local.conf'
$redisData = ConvertTo-CygwinPath $dataDirectory
$redisLog = ConvertTo-CygwinPath (Join-Path $localRoot 'redis-local.log')
@"
bind 127.0.0.1
port $Port
protected-mode yes
daemonize no
dir "$redisData"
logfile "$redisLog"
appendonly yes
appendfsync everysec
save 60 100
maxmemory 128mb
maxmemory-policy noeviction
"@ | Set-Content -LiteralPath $config -Encoding ascii
$redisConfig = ConvertTo-CygwinPath $config
$process = Start-Process -FilePath $server.FullName -WorkingDirectory $server.DirectoryName -ArgumentList ('"' + $redisConfig + '"') -WindowStyle Hidden -PassThru
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    if (Test-RedisPing) { Write-Output "Redis local started at 127.0.0.1:$Port (PID $($process.Id))"; return }
    if ($process.HasExited) { throw 'Redis stopped during startup. See project-data/.local/redis-local.log.' }
    Start-Sleep -Milliseconds 300
}
throw 'Redis did not become ready. See project-data/.local/redis-local.log.'
