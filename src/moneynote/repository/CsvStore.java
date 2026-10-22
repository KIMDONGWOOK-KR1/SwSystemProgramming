package moneynote.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV 읽기/쓰기 공통 처리. (F10)
 *
 * <p>메모에는 쉼표나 따옴표가 들어올 수 있으므로 RFC 4180 규칙으로 이스케이프한다.
 * 필드에 쉼표, 따옴표, 줄바꿈이 있으면 전체를 따옴표로 감싸고 내부 따옴표는 두 번 반복한다.
 */
public final class CsvStore {

    private static final char DELIMITER = ',';
    private static final char QUOTE = '"';

    private CsvStore() {
    }

    /**
     * 헤더를 제외한 데이터 행을 읽는다. 빈 줄은 건너뛴다.
     *
     * @param file CSV 파일
     * @return 행마다 필드 배열, 파일이 없으면 빈 목록(최초 실행)
     * @throws IOException 파일을 읽지 못한 경우
     */
    public static List<String[]> read(Path file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(file)) {
            return rows;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            rows.add(parseLine(line));
        }
        return rows;
    }

    /**
     * 헤더와 행을 UTF-8로 쓴다. 폴더가 없으면 만든다.
     *
     * @param file   CSV 파일
     * @param header 첫 줄에 쓸 열 이름
     * @param rows   데이터 행
     * @throws IOException 파일을 쓰지 못한 경우
     */
    public static void write(Path file, String[] header, List<String[]> rows) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(join(header)).append(System.lineSeparator());
        for (String[] row : rows) {
            sb.append(join(row)).append(System.lineSeparator());
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    static String join(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(DELIMITER);
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    static String escape(String field) {
        String value = field == null ? "" : field;
        boolean needsQuote = value.indexOf(DELIMITER) >= 0
                || value.indexOf(QUOTE) >= 0
                || value.contains("\n");
        if (!needsQuote) {
            return value;
        }
        return QUOTE + value.replace("\"", "\"\"") + QUOTE;
    }

    /** 한 줄을 필드 배열로 나눈다. 따옴표로 감싼 필드 안의 쉼표는 구분자로 보지 않는다. */
    static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == QUOTE) {
                    if (i + 1 < line.length() && line.charAt(i + 1) == QUOTE) {
                        current.append(QUOTE);
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == QUOTE) {
                inQuotes = true;
            } else if (c == DELIMITER) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
