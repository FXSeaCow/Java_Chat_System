$ErrorActionPreference = 'Stop'
$classDir = Join-Path $PSScriptRoot 'out\admin-client'
$sources = @(
    Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'common\src\main\java'),
                               (Join-Path $PSScriptRoot 'admin-client\src\main\java') -Recurse -Filter '*.java' |
        ForEach-Object FullName
)

New-Item -ItemType Directory -Force -Path $classDir | Out-Null
& javac -encoding UTF-8 -d $classDir $sources
if ($LASTEXITCODE -ne 0) { throw 'Admin client compilation failed.' }

& java -cp $classDir com.javachat.admin.AdminClientApp
if ($LASTEXITCODE -ne 0) { throw 'Admin client exited with an error.' }
