param([switch]$MySql,[int]$DatabasePort = 3307,[switch]$Container)
$ErrorActionPreference = 'Stop'
$dataRoot = Split-Path $PSScriptRoot -Parent
$projectRoot = Split-Path $dataRoot -Parent
Push-Location (Join-Path $projectRoot 'backend')
try {
    $mavenArgs = @("-Dmaven.repo.local=$dataRoot\.m2\repository",'-Ddebug=false','-B','-ntp','verify')
    if ($MySql) {
        # Dedicated test database only; never point this at application/user data.
        $mavenArgs += @("-Dspring.datasource.url=jdbc:mysql://127.0.0.1:$DatabasePort/fashion_store_test",'-Dspring.datasource.username=fashion','-Dspring.datasource.password=fashion','-Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver')
    }
    if ($Container) { $mavenArgs += '-Dspring.profiles.active=container-test' }
    & .\mvnw.cmd @mavenArgs
    if ($LASTEXITCODE -ne 0) { throw 'Backend checks failed.' }
} finally { Pop-Location }
Push-Location (Join-Path $projectRoot 'frontend')
try {
    npm run lint
    if ($LASTEXITCODE -ne 0) { throw 'Frontend lint failed.' }
    npm run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
    npm run test
    if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed.' }
} finally { Pop-Location }
