# MoneyNote 빌드 및 실행 스크립트
#
#   .\run.ps1          컴파일 후 프로그램 실행
#   .\run.ps1 -Test    컴파일 후 서비스 계층 검증 하네스 실행
#
# 빌드 도구 없이 JDK 만으로 동작한다.

param(
    [switch]$Test
)

$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
$out = Join-Path $root "out"

# 데이터 파일 경로가 상대 경로이므로 항상 프로젝트 폴더에서 실행한다.
Push-Location $root
try {
    $sources = Get-ChildItem -Recurse -Filter *.java -Path (Join-Path $root "src"), (Join-Path $root "test") |
        ForEach-Object { $_.FullName }

    Write-Host "컴파일 중... ($($sources.Count) 개 파일)"
    javac -encoding UTF-8 -d $out $sources
    if ($LASTEXITCODE -ne 0) {
        Write-Host "컴파일 실패"
        exit $LASTEXITCODE
    }

    if ($Test) {
        # 콘솔 코드 페이지와 무관하게 한글이 보이도록 출력 인코딩을 지정한다.
        java "-Dfile.encoding=UTF-8" "-Dstdout.encoding=UTF-8" -cp $out SmokeTest
        exit $LASTEXITCODE
    }

    java "-Dfile.encoding=UTF-8" -cp $out moneynote.Main
}
finally {
    Pop-Location
}
