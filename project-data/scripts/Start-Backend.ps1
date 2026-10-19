param([int]$DatabasePort = 3307,[string]$AdminEmail = 'admin@forme.local',[string]$AdminPassword = 'FormeLocalAdmin2026!')
$ErrorActionPreference = 'Stop'
$dataRoot = Split-Path $PSScriptRoot -Parent
$projectRoot = Split-Path $dataRoot -Parent
$mailConfig = Join-Path $dataRoot '.local/mail.properties'
if ((Test-Path -LiteralPath $mailConfig) -and -not $env:SPRING_CONFIG_ADDITIONAL_LOCATION) {
    $env:SPRING_CONFIG_ADDITIONAL_LOCATION = ([System.Uri]$mailConfig).AbsoluteUri
}
if (-not $env:DB_URL) { $env:DB_URL = "jdbc:mysql://127.0.0.1:$DatabasePort/fashion_store?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true" }
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = 'fashion' }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = 'fashion' }
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:LOCAL_ADMIN_EMAIL = $AdminEmail
$env:LOCAL_ADMIN_PASSWORD = $AdminPassword
Push-Location (Join-Path $projectRoot 'backend')
try {
    & .\mvnw.cmd "-Dmaven.repo.local=$dataRoot\.m2\repository" -Ddebug=false -B -ntp '-Dspring-boot.run.jvmArguments=-Ddebug=false' spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw 'Backend failed to start. See the Maven output.' }
} finally { Pop-Location }
