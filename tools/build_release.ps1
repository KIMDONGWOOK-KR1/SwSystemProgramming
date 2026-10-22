# 체크포인트 4 제출물 생성 스크립트
#
#   .\tools\build_release.ps1 -StudentId 220057
#
# dist\ 아래에 다음을 만든다. 과제 공지의 파일명 규칙을 따른다.
#   ssd_ip_cp4_2026f_[학번]_app.jar         실행 가능한 jar (java -jar 또는 더블클릭)
#   ssd_ip_cp4_2026f_[학번]_api_manual.zip  Javadoc API 매뉴얼 (api_manual\index.html)
#   ssd_ip_cp4_2026f_[학번]_src.zip         소스 코드 (src, test, tools, run.ps1, README.md)
#   ssd_ip_cp4_2026f_[학번].zip             위 셋과 보고서 PDF 를 묶은 최종 제출 파일
#
# 보고서 PDF(ssd_ip_cp4_2026f_[학번]_report.pdf)는 프로젝트 폴더에 먼저 두어야 최종 zip 에 들어간다.
# PDF 는 Word 에서 직접 내보낸다. 없으면 경고만 하고 나머지는 만든다.
#
# jar 는 Java 17 이상에서 돌도록 --release 17 로 컴파일한다. 채점 환경의 JDK 가 21 보다 낮을 수 있어서다.
# zip 은 jar 도구로 만든다. PowerShell 5.1 의 Compress-Archive 는 경로 구분자를 \ 로 기록해
# 다른 운영체제에서 폴더 구조가 깨진다.

param(
    [string]$StudentId = "220057"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $root "dist"
$work = Join-Path $dist "work"
$prefix = "ssd_ip_cp4_2026f_$StudentId"

function Invoke-Tool {
    param([string]$exe, [string[]]$arguments)
    & $exe @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$exe 실패 (종료 코드 $LASTEXITCODE)"
    }
}

if (Test-Path $dist) {
    Remove-Item -Recurse -Force $dist
}
New-Item -ItemType Directory -Force (Join-Path $work "classes") | Out-Null

Push-Location $root
try {
    $sources = Get-ChildItem -Recurse -Filter *.java -Path (Join-Path $root "src") |
        ForEach-Object { $_.FullName }

    # 1. 실행 가능한 jar
    Write-Host "컴파일 중... ($($sources.Count) 개 파일)"
    Invoke-Tool javac (@("--release", "17", "-encoding", "UTF-8", "-d", (Join-Path $work "classes")) + $sources)
    $jar = Join-Path $dist "${prefix}_app.jar"
    Invoke-Tool jar @("--create", "--file", $jar, "--main-class", "moneynote.Main",
        "-C", (Join-Path $work "classes"), ".")

    # 2. API 매뉴얼
    Write-Host "Javadoc 생성 중..."
    $apiDir = Join-Path $work "api\api_manual"
    Invoke-Tool javadoc @("-quiet", "-Xdoclint:all", "-encoding", "UTF-8", "-charset", "UTF-8",
        "-docencoding", "UTF-8", "--release", "17",
        "-windowtitle", "MoneyNote API", "-doctitle", "MoneyNote 개인 가계부 API 매뉴얼",
        "-d", $apiDir, "-sourcepath", "src", "-subpackages", "moneynote")
    Invoke-Tool jar @("--create", "--no-manifest", "--file", (Join-Path $dist "${prefix}_api_manual.zip"),
        "-C", (Join-Path $work "api"), "api_manual")

    # 3. 소스 코드
    $srcRoot = Join-Path $work "src\$prefix"
    New-Item -ItemType Directory -Force $srcRoot | Out-Null
    foreach ($item in @("src", "test", "tools", "run.ps1", "README.md")) {
        Copy-Item -Recurse -Path (Join-Path $root $item) -Destination $srcRoot
    }
    Invoke-Tool jar @("--create", "--no-manifest", "--file", (Join-Path $dist "${prefix}_src.zip"),
        "-C", (Join-Path $work "src"), $prefix)

    # 4. 최종 제출 zip
    $report = Join-Path $root "${prefix}_report.pdf"
    $bundle = Join-Path $work "bundle"
    New-Item -ItemType Directory -Force $bundle | Out-Null
    Copy-Item (Join-Path $dist "${prefix}_app.jar"), (Join-Path $dist "${prefix}_api_manual.zip"),
        (Join-Path $dist "${prefix}_src.zip") -Destination $bundle
    if (Test-Path $report) {
        Copy-Item $report -Destination $bundle
    } else {
        Write-Warning "보고서 PDF 가 없습니다: $report  (Word 에서 내보낸 뒤 다시 실행하세요)"
    }
    $final = Join-Path $dist "$prefix.zip"
    Invoke-Tool jar @("--create", "--no-manifest", "--file", $final, "-C", $bundle, ".")

    Remove-Item -Recurse -Force $work
    Write-Host ""
    Get-ChildItem $dist | ForEach-Object {
        Write-Host ("{0,-45} {1,8:N0} KB" -f $_.Name, ($_.Length / 1KB))
    }
    $sizeMb = (Get-Item $final).Length / 1MB
    Write-Host ("최종 zip 크기: {0:N2} MB (제한 50 MB)" -f $sizeMb)
}
finally {
    Pop-Location
}
