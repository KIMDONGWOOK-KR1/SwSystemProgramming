# 보고서 생성 스크립트들이 dot-source 해서 쓰는 Word COM 헬퍼.
#
#   . (Join-Path $PSScriptRoot "word_helpers.ps1")

# 본문 스타일 이름. 원본 양식이 한국어 Word로 저장되어 있어 한국어 스타일명을 쓴다.
$BODY = "본문"
$H1 = "제목 1"
$H2 = "제목 2"

function Add-Para {
    param($doc, [string]$text, [string]$style)

    $doc.Content.InsertParagraphAfter()
    $last = $doc.Paragraphs.Item($doc.Paragraphs.Count)
    if ($text) {
        $last.Range.InsertBefore($text)
    }
    try {
        $doc.Paragraphs.Item($doc.Paragraphs.Count).Style = $style
    } catch {
        # 해당 이름의 스타일이 없으면 기본 본문으로 떨어뜨린다.
        $doc.Paragraphs.Item($doc.Paragraphs.Count).Style = $BODY
    }
}

function Set-ParagraphText {
    param($doc, [int]$index, [string]$text)

    $range = $doc.Paragraphs.Item($index).Range
    # wdCharacter = 1. 끝을 한 글자 줄여 문단 기호를 범위에서 제외한다.
    $null = $range.MoveEnd(1, -1)
    $range.Text = $text
}

function Add-Table {
    param($doc, $rows, [string]$caption)

    # 표 캡션은 관례대로 표 위에 둔다.
    if ($caption) {
        Add-Para $doc $caption $BODY
    }

    $doc.Content.InsertParagraphAfter()
    $anchor = $doc.Paragraphs.Item($doc.Paragraphs.Count)

    $rowCount = $rows.Count
    $colCount = $rows[0].Count
    $table = $doc.Tables.Add($anchor.Range, $rowCount, $colCount)

    $table.Borders.InsideLineStyle = 1
    $table.Borders.OutsideLineStyle = 1
    $table.Range.Font.Size = 8
    $table.Range.ParagraphFormat.SpaceAfter = 0
    $table.PreferredWidthType = 2   # wdPreferredWidthPercent
    $table.PreferredWidth = 100

    for ($r = 0; $r -lt $rowCount; $r++) {
        for ($c = 0; $c -lt $colCount; $c++) {
            $table.Cell($r + 1, $c + 1).Range.Text = $rows[$r][$c]
        }
    }
    $table.Rows.Item(1).Range.Bold = $true
    $table.Rows.Item(1).HeadingFormat = $true
}
