$ErrorActionPreference = 'Stop'
$classDir = Join-Path $PSScriptRoot 'out\server'
$driver = Join-Path $env:USERPROFILE '.m2\repository\org\postgresql\postgresql\42.7.14\postgresql-42.7.14.jar'
if (-not (Test-Path -LiteralPath $driver)) {
    throw "PostgreSQL JDBC driver missing: $driver. Build the server module with Maven first."
}
$sources = @(
    Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'common\src\main\java'),
                               (Join-Path $PSScriptRoot 'server\src\main\java') -Recurse -Filter '*.java' |
        ForEach-Object FullName
)
New-Item -ItemType Directory -Force -Path $classDir | Out-Null
& javac -encoding UTF-8 -d $classDir $sources
if ($LASTEXITCODE -ne 0) { throw 'Server compilation failed.' }
& java -cp "$classDir;$driver" com.javachat.server.ServerApp
if ($LASTEXITCODE -ne 0) { throw 'Server exited with an error.' }
