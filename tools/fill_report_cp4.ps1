# 체크포인트 4 보고서 생성 스크립트
#
#   .\tools\fill_report_cp4.ps1 -Name "홍길동" -StudentId "202412345"
#
# 내용은 docs/checkpoint4_report.md 에서 읽는다. 문구를 고칠 때는 md 만 고치면 된다.
# 체크포인트 4 전용 양식이 없어 체크포인트 3 양식(ssd_ip_checkpoint3_report_format.doc)의 형식을 그대로 따른다.
#   ## 제목        -> 제목 1 (양식의 섹션 이름과 같아야 한다)
#   - 글           -> bullet list (양식의 "Use this bullet for listing ...")
#   > 글           -> 글머리 기호 없는 bullet list 줄 (양식의 ": This function do something.." 줄)
#   ![캡션](경로)  -> 단 폭 그림 + figure caption (양식 스타일이 "Fig. N." 을 자동으로 붙인다)
#   나머지         -> 본문 문단 (굵게 **, 코드 ` 는 벗겨낸다)
# 첫 ## 앞의 제목·저자 줄과 <!-- --> 메모는 읽지 않는다. 제목과 저자는 아래 매개변수로 채운다.
#
# 그림은 미리 만들어 둔다.
#   python tools\uml_diagrams.py docs\cp4_figures
#   javac -encoding UTF-8 -cp out -d out\tools tools\GuiSketch.java tools\BetaScreenshots.java
#   java -cp "out;out\tools" GuiSketch docs\cp4_figures
#   java -cp "out;out\tools" BetaScreenshots docs\cp4_figures   (실제 창을 띄워 찍으므로 가리지 않는다)
# API 매뉴얼 그림(api_*.png)은 build_release.ps1 이 만든 Javadoc 을 Edge 헤드리스로 찍은 것이다.
#
# 양식은 사본을 쓰고 원본은 건드리지 않는다.
# PDF 는 생성된 doc 를 Word 에서 열어 직접 내보낸다. COM 에서는 PDF 변환이 응답하지 않는다.
#
# 제출 파일명 규칙: ssd_ip_cp4_2026f_[학번]_report.pdf. 제출 기한: 2026년 11월 5일.
# PDF 를 프로젝트 폴더에 둔 뒤 build_release.ps1 을 다시 돌리면 최종 zip 에 들어간다.

param(
    [string]$Name = "[이름]",
    [string]$StudentId = "[학번]",
    [string]$AppName = "MoneyNote: 개인 가계부 관리 프로그램"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$template = Join-Path $root "ssd_ip_checkpoint3_report_format.doc"
$source = Join-Path $root "docs\checkpoint4_report.md"
$docsDir = Join-Path $root "docs"

if ($StudentId -match "^[0-9]+$") {
    $baseName = "ssd_ip_cp4_2026f_${StudentId}_report"
} else {
    $baseName = "checkpoint4_보고서_초안"
    Write-Warning "학번이 없어 초안 이름으로 저장합니다. 제출용 파일은 -StudentId 를 지정해야 만들어집니다."
}
$output = Join-Path $root "$baseName.doc"

if (-not (Test-Path $template)) {
    throw "원본 양식을 찾을 수 없습니다: $template"
}

Copy-Item -Path $template -Destination $output -Force

. (Join-Path $PSScriptRoot "word_helpers.ps1")

$BULLET = "bullet list"
# 함수 인자 $caption 과 이름이 겹치면 PowerShell 은 대소문자를 구분하지 않아 가려지므로 이름을 달리한다.
$CAPTION_STYLE = "figure caption"

function Clean([string]$text) {
    return ($text -replace '\*\*', '' -replace '`', '').Trim()
}

# 양식의 ": This function do something.." 줄처럼 bullet list 스타일에서 기호만 뗀 문단.
function Add-ContinuationLine {
    param($doc, [string]$text)

    Add-Para $doc $text $BULLET
    $para = $doc.Paragraphs.Item($doc.Paragraphs.Count)
    $para.Range.ListFormat.RemoveNumbers()
    # RemoveNumbers 가 들여쓰기도 지우므로 양식의 값(왼쪽 28.8pt, 첫 줄 0)을 다시 준다.
    $para.LeftIndent = 28.8
    $para.FirstLineIndent = 0
}

function Add-Figure {
    param($doc, [string]$path, [string]$caption, [double]$maxWidth, [double]$maxHeight)

    Add-Para $doc "" $BODY
    $para = $doc.Paragraphs.Item($doc.Paragraphs.Count)
    $para.Alignment = 1
    $para.FirstLineIndent = 0
    $para.SpaceBefore = 6
    $para.SpaceAfter = 0
    $para.LineSpacingRule = 0
    # 그림과 캡션이 단이나 쪽 경계에서 갈라지지 않게 한다.
    $para.KeepWithNext = -1
    $shape = $doc.InlineShapes.AddPicture($path, $false, $true, $para.Range)
    $shape.LockAspectRatio = -1
    $ratio = $shape.Height / $shape.Width
    # 그림은 2배율(192dpi)로 저장되어 있다. 작은 대화상자 그림이 지나치게 커지지 않게
    # 원래 1배 픽셀 수의 0.5pt/px 를 상한으로 둔다.
    $width = [Math]::Min($maxWidth, $shape.Width * 0.5 * 96 / 72)
    if ($width * $ratio -gt $maxHeight) {
        $width = $maxHeight / $ratio
    }
    $shape.Width = $width

    Add-Para $doc $caption $BODY
    $cap = $doc.Paragraphs.Item($doc.Paragraphs.Count)
    $cap.Style = $CAPTION_STYLE
    # 양식의 예시 캡션처럼 단 가운데에 둔다.
    $cap.Alignment = 1
    $cap.SpaceAfter = 10
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

    # 9번 문단(양식의 첫 제목 1)만 남기고 지시문과 예시 그림을 지운다. 첫 ## 제목이 이 문단을 차지한다.
    for ($i = $doc.Paragraphs.Count; $i -ge 10; $i--) {
        $doc.Paragraphs.Item($i).Range.Delete() | Out-Null
    }

    $setup = $doc.Sections.Item($doc.Sections.Count).PageSetup
    # 양식의 예시 그림(약 230pt)처럼 단 폭 안에 넣는다.
    $figureWidth = $setup.TextColumns.Width - 6
    $maxHeight = ($setup.PageHeight - $setup.TopMargin - $setup.BottomMargin) * 0.8

    $firstHeading = $true
    foreach ($line in $lines) {
        if ($line -match '^!\[(.+?)\]\((.+?)\)\s*$') {
            $path = Join-Path $docsDir ($Matches[2] -replace '/', '\')
            if (-not (Test-Path $path)) {
                throw "그림을 찾을 수 없습니다: $path"
            }
            Add-Figure $doc $path $Matches[1] $figureWidth $maxHeight
        } elseif ($line.StartsWith("## ")) {
            if ($firstHeading) {
                Set-ParagraphText $doc 9 (Clean $line.Substring(3))
                $firstHeading = $false
            } else {
                Add-Para $doc (Clean $line.Substring(3)) $H1
            }
        } elseif ($line.StartsWith("- ")) {
            Add-Para $doc (Clean $line.Substring(2)) $BULLET
        } elseif ($line.StartsWith("> ")) {
            Add-ContinuationLine $doc (Clean $line.Substring(2))
        } elseif ($line.Trim()) {
            Add-Para $doc (Clean $line) $BODY
        }
    }

    Set-ParagraphText $doc 3 "$Name, $StudentId"
    Set-ParagraphText $doc 1 $AppName

    $doc.Save()
    Write-Host "생성 완료: $output"
    Write-Host "문단 수: $($doc.Paragraphs.Count), 표: $($doc.Tables.Count), 그림: $($doc.InlineShapes.Count), 구역: $($doc.Sections.Count), 페이지: $($doc.ComputeStatistics(2))"

    $doc.Close($false)
}
finally {
    $word.Quit()
    [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($word)
    [GC]::Collect()
}
