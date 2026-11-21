$ErrorActionPreference = 'Stop'
$classDir = Join-Path $PSScriptRoot 'out\user-client-demo'
$sources = @(
    Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'common\src\main\java'),
                               (Join-Path $PSScriptRoot 'user-client\src\main\java') -Recurse -Filter '*.java' |
        ForEach-Object FullName
)

New-Item -ItemType Directory -Force -Path $classDir | Out-Null
& javac -encoding UTF-8 -d $classDir $sources
if ($LASTEXITCODE -ne 0) { throw 'Biên dịch Java thất bại.' }

& java -cp $classDir com.javachat.user.UserClientApp --demo
if ($LASTEXITCODE -ne 0) { throw 'Không thể chạy E-Chat demo.' }
