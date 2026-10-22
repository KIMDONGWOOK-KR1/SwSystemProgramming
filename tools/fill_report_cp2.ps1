# 체크포인트 2 보고서 생성 스크립트
#
#   .\tools\fill_report_cp2.ps1 -Name "홍길동" -StudentId "202412345"
#
# 내용은 docs/checkpoint2_report.md 에서 읽는다. 문구를 고칠 때는 md 만 고치면 된다.
#   ## 제목    -> 제목 1
#   ### 제목   -> 제목 2
#   | 표 |     -> Word 표 (구분선 |---| 은 건너뛴다)
#   나머지     -> 본문 문단 (굵게 **, 코드 `, 목록 기호 - 는 벗겨낸다)
# 첫 ## 앞의 제목과 저자 줄은 양식의 제목 문단에 따로 채운다.
#
# 양식은 1차와 같은 ssd_ip_checkpoint1_report_format.doc 의 사본을 쓴다. 원본은 건드리지 않는다.
# PDF 는 생성된 doc 를 Word 에서 열어 직접 내보낸다. COM 에서는 PDF 변환이 응답하지 않는다.
#
# 제출 파일명 규칙: ssd_ip_cp2_2026f_[학번].{doc|pdf}. 제출 기한: 2026년 10월 1일.

param(
    [string]$Name = "[이름]",
    [string]$StudentId = "[학번]",
    [string]$AppName = "MoneyNote: 개인 가계부 관리 프로그램"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$template = Join-Path $root "ssd_ip_checkpoint1_report_format.doc"
$source = Join-Path $root "docs\checkpoint2_report.md"

if ($StudentId -match "^[0-9]+$") {
    $baseName = "ssd_ip_cp2_2026f_$StudentId"
} else {
    $baseName = "checkpoint2_보고서_초안"
    Write-Warning "학번이 없어 초안 이름으로 저장합니다. 제출용 파일은 -StudentId 를 지정해야 만들어집니다."
}
$output = Join-Path $root "$baseName.doc"

if (-not (Test-Path $template)) {
    throw "원본 양식을 찾을 수 없습니다: $template"
}

Copy-Item -Path $template -Destination $output -Force

. (Join-Path $PSScriptRoot "word_helpers.ps1")

function Clean([string]$text) {
    return ($text -replace '\*\*', '' -replace '`', '' -replace '^- ', '').Trim()
}

$lines = Get-Content -Path $source -Encoding UTF8
$start = [Array]::FindIndex($lines, [Predicate[string]] { param($l) $l.StartsWith("## ") })
$lines = $lines[$start..($lines.Count - 1)]

Write-Host "Word 시작..."
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0

try {
    $doc = $word.Documents.Open($output, $false, $false)

    # 9번 문단(양식의 첫 제목 1)만 남기고 지시문을 지운다. 첫 ## 제목이 이 문단을 차지한다.
    for ($i = $doc.Paragraphs.Count; $i -ge 10; $i--) {
        $doc.Paragraphs.Item($i).Range.Delete() | Out-Null
    }

    $firstHeading = $true
    $tableRows = @()

    # 끝에 빈 줄을 하나 붙여 마지막 표도 같은 경로로 flush 되게 한다.
    foreach ($line in $lines + "") {
        if ($line.StartsWith("|")) {
            if ($line -notmatch '^\|[-| ]+\|$') {
                # 흐름 칸의 "1. … 2. …", "A1. …" 단계마다 칸 안에서 줄을 바꾼다 (`v = Word 줄 바꿈).
                $cells = $line.Trim().Trim("|").Split("|") |
                    ForEach-Object { (Clean $_) -replace ' (?=([0-9]+|[A-Z][0-9])\. )', "`v" }
                $tableRows += , @($cells)
            }
            continue
        }
        if ($tableRows.Count -gt 0) {
            Add-Table $doc $tableRows $null
            $tableRows = @()
        }

        if ($line.StartsWith("### ")) {
            Add-Para $doc (Clean $line.Substring(4)) $H2
        } elseif ($line.StartsWith("## ")) {
            if ($firstHeading) {
                Set-ParagraphText $doc 9 (Clean $line.Substring(3))
                $firstHeading = $false
            } else {
                Add-Para $doc (Clean $line.Substring(3)) $H1
            }
        } elseif ($line.Trim() -and $line.Trim() -ne "---") {
            Add-Para $doc (Clean $line) $BODY
        }
    }

    # 표 머리행으로 표를 알아보고 열 폭(%)을 정한다. 긴 글이 들어가는 열에 폭을 몰아준다.
    $widths = @{
        "번호|기능|포함한 1차 기능|유스케이스" = 10, 27, 42, 21
        "ID|요구사항|기능"                    = 13, 74, 13
        "ID|요구사항"                         = 15, 85
        "유스케이스|관련 요구사항"            = 55, 45
        "항목|내용"                           = 22, 78
    }
    foreach ($table in $doc.Tables) {
        $header = (1..$table.Columns.Count | ForEach-Object {
            $table.Cell(1, $_).Range.Text -replace "[\r\a]", ""
        }) -join "|"
        if ($widths.ContainsKey($header)) {
            $w = $widths[$header]
            for ($c = 1; $c -le $w.Count; $c++) {
                $table.Columns.Item($c).PreferredWidthType = 2
                $table.Columns.Item($c).PreferredWidth = $w[$c - 1]
            }
        }
        # 양식의 본문 스타일은 가운데 정렬에 줄 간격이 넓다. 표 안은 왼쪽 정렬, 한 줄 간격으로 둔다.
        $table.Range.ParagraphFormat.Alignment = 0
        $table.Range.ParagraphFormat.LineSpacingRule = 0
        $table.Range.ParagraphFormat.SpaceBefore = 0
        $table.Rows.Item(1).Range.ParagraphFormat.Alignment = 1
        # 일부 행에서 구분선이 빠져 보여 모든 칸에 테두리를 직접 켠다.
        $table.Range.Cells.Borders.Enable = 1
        # 행이 단이나 쪽 경계에서 쪼개지지 않게 한다.
        $table.Rows.AllowBreakAcrossPages = 0
    }

    Set-ParagraphText $doc 3 "$Name, $StudentId"
    Set-ParagraphText $doc 1 $AppName

    $doc.Save()
    Write-Host "생성 완료: $output"
    Write-Host "문단 수: $($doc.Paragraphs.Count), 표: $($doc.Tables.Count), 페이지: $($doc.ComputeStatistics(2))"

    $doc.Close($false)
}
finally {
    $word.Quit()
    [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($word)
    [GC]::Collect()
}
