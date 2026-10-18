param([int]$Port = 3307, [string]$RootPassword = 'local-root-change-me-2026')
$ErrorActionPreference = 'Stop'
$dataRoot = Split-Path $PSScriptRoot -Parent
$projectRoot = Split-Path $dataRoot -Parent
$localDir = Join-Path $dataRoot '.local'
$dataDir = Join-Path $localDir 'mysql-data'
New-Item -ItemType Directory -Force -Path $localDir | Out-Null
$serverCommand = Get-Command mysqld.exe -ErrorAction SilentlyContinue
if ($serverCommand) { $mysqlBin = Split-Path $serverCommand.Source -Parent }
else {
    $installation = Get-ChildItem -LiteralPath "$env:ProgramFiles\MySQL" -Directory -ErrorAction SilentlyContinue |
        Where-Object Name -Like 'MySQL Server*' | Sort-Object Name -Descending | Select-Object -First 1
    if (-not $installation) { throw 'Install MySQL Server 8.0+ or add its bin directory to PATH.' }
    $mysqlBin = Join-Path $installation.FullName 'bin'
}
$server = Join-Path $mysqlBin 'mysqld.exe'
$client = Join-Path $mysqlBin 'mysql.exe'
if (-not (Test-Path -LiteralPath (Join-Path $dataDir 'auto.cnf'))) {
    & $server --no-defaults --initialize-insecure "--datadir=$dataDir" --console
    if ($LASTEXITCODE -ne 0) { throw 'MySQL initialization failed. See the output above.' }
    $fresh = $true
} else { $fresh = $false }
$adminClient = Join-Path $mysqlBin 'mysqladmin.exe'
$savedPassword = $env:MYSQL_PWD
if ($fresh) { $env:MYSQL_PWD = '' } else { $env:MYSQL_PWD = $RootPassword }
& $adminClient --host=127.0.0.1 "--port=$Port" --user=root --connect-timeout=1 ping 2>&1 | Out-Null
$running = $LASTEXITCODE -eq 0
if ($running) {
    $actualData = & $client --host=127.0.0.1 "--port=$Port" --user=root --batch --skip-column-names --execute='SELECT @@datadir' 2>&1
    if ($LASTEXITCODE -ne 0) {
        $env:MYSQL_PWD = $savedPassword
        throw 'A server is already running on this port; its root password could not be verified.'
    }
    $actualPath = [IO.Path]::GetFullPath([string]($actualData | Select-Object -Last 1)).TrimEnd('\','/')
    if ($actualPath -ine [IO.Path]::GetFullPath($dataDir).TrimEnd('\','/')) {
        $env:MYSQL_PWD = $savedPassword
        throw "Port $Port belongs to a different MySQL data directory. Select another port."
    }
} else {
    $arguments = "--no-defaults --datadir=`"$dataDir`" --port=$Port --bind-address=127.0.0.1 --mysqlx=0 --skip-log-bin --console"
    Start-Process -FilePath $server -ArgumentList $arguments -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $localDir 'mysql.stdout.log') `
        -RedirectStandardError (Join-Path $localDir 'mysql.stderr.log') | Out-Null
    $timer = [Diagnostics.Stopwatch]::StartNew()
    do {
        Start-Sleep -Milliseconds 300
        & $adminClient --host=127.0.0.1 "--port=$Port" --user=root --connect-timeout=1 ping 2>&1 | Out-Null
        $running = $LASTEXITCODE -eq 0
        if ($timer.Elapsed.TotalSeconds -gt 30) {
            $env:MYSQL_PWD = $savedPassword
            throw 'MySQL did not start. Inspect project-data/.local/mysql.stderr.log.'
        }
    } while (-not $running)
}
try {
    if ($fresh) { $env:MYSQL_PWD = '' } else { $env:MYSQL_PWD = $RootPassword }
    $sql = @"
CREATE DATABASE IF NOT EXISTS fashion_store CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS fashion_store_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'fashion'@'localhost' IDENTIFIED BY 'fashion';
GRANT ALL ON fashion_store.* TO 'fashion'@'localhost';
GRANT ALL ON fashion_store_test.* TO 'fashion'@'localhost';
"@
    if ($fresh) {
        $escapedPassword = $RootPassword.Replace('\', '\\').Replace("'", "''")
        $sql += "`nALTER USER 'root'@'localhost' IDENTIFIED BY '$escapedPassword';"
    }
    $sql | & $client --host=127.0.0.1 "--port=$Port" --user=root
    if ($LASTEXITCODE -ne 0) { throw 'Database setup failed. Check -RootPassword and the MySQL logs.' }
} finally { $env:MYSQL_PWD = $savedPassword }
Write-Host "Project MySQL is ready on 127.0.0.1:$Port. Databases: fashion_store, fashion_store_test."
