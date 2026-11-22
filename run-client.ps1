$ErrorActionPreference = 'Stop'
$classDir = Join-Path $PSScriptRoot 'out\user-client-live'
$resourcesDir = Join-Path $PSScriptRoot 'user-client\src\main\resources'
$sources = @(
    Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'common\src\main\java'),
                               (Join-Path $PSScriptRoot 'user-client\src\main\java') -Recurse -Filter '*.java' |
        ForEach-Object FullName
)
New-Item -ItemType Directory -Force -Path $classDir | Out-Null
& javac -encoding UTF-8 -d $classDir $sources
if ($LASTEXITCODE -ne 0) { throw 'Client compilation failed.' }
& java -cp "$classDir;$resourcesDir" com.javachat.user.UserClientApp
if ($LASTEXITCODE -ne 0) { throw 'Client exited with an error.' }
