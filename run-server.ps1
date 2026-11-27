$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try {
    & mvn -q -pl server -am package '-DskipTests'
    if ($LASTEXITCODE -ne 0) { throw 'Server build failed.' }
    & java -cp 'server/target/classes;server/target/dependency/*' com.javachat.server.ServerApp
    if ($LASTEXITCODE -ne 0) { throw 'Server exited with an error.' }
} finally {
    Pop-Location
}
