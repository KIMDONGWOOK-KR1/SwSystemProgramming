package moneynote.repository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import moneynote.model.RecurringRule;

/**
 * 고정 지출 규칙 저장소. (F9)
 *
 * <p>규칙과 함께 마지막 생성 월도 저장한다. 이 값이 없으면 프로그램을 켤 때마다 같은 달의
 * 거래가 다시 만들어지고, 사용자가 지운 자동 거래가 되살아난다.
 */
public class RecurringRepository {

    private static final String[] HEADER = {
        "id", "categoryId", "amount", "dayOfMonth", "memo", "active", "lastGenerated"
    };

    private final Path file;
    private final List<RecurringRule> items = new ArrayList<>();
    private final List<Integer> skippedRows = new ArrayList<>();
    private int nextId = 1;

    /**
     * 저장소를 만든다. 파일은 {@link #load()} 를 부를 때 읽는다.
     *
     * @param file 규칙을 저장할 CSV 파일 경로
     */
    public RecurringRepository(Path file) {
        this.file = file;
    }

    /**
     * 등록 순서대로 모든 규칙 (읽기 전용).
     *
     * @return 등록 순서대로 모든 규칙 (읽기 전용)
     */
    public List<RecurringRule> findAll() {
        return List.copyOf(items);
    }

    /**
     * id로 규칙을 찾는다.
     *
     * @param id 규칙 id
     * @return 해당 규칙, 없으면 빈 값
     */
    public Optional<RecurringRule> findById(int id) {
        return items.stream().filter(r -> r.id() == id).findFirst();
    }

    /**
     * 분류를 쓰는 규칙 수.
     *
     * @param categoryId 분류 id
     * @return 이 분류를 쓰는 규칙 수
     */
    public int countByCategory(int categoryId) {
        return (int) items.stream().filter(r -> r.categoryId() == categoryId).count();
    }

    /**
     * 새 id를 붙여 활성 규칙을 추가한다. 값 검증은 서비스 계층이 한다.
     *
     * @param categoryId 지출 분류 id
     * @param amount     매월 금액(원)
     * @param dayOfMonth 매월 결제일
     * @param memo       생성될 거래의 메모
     * @return 추가된 규칙
     */
    public RecurringRule add(int categoryId, long amount, int dayOfMonth, String memo) {
        RecurringRule rule = new RecurringRule(nextId++, categoryId, amount, dayOfMonth, memo,
                true, null);
        items.add(rule);
        return rule;
    }

    /**
     * 규칙을 지운다. 없으면 아무 일도 하지 않는다.
     *
     * @param id 지울 규칙 id
     */
    public void remove(int id) {
        items.removeIf(r -> r.id() == id);
    }

    /**
     * 형식이 깨져 건너뛴 행.
     *
     * @return 마지막 {@link #load()} 에서 건너뛴 행 번호(헤더를 1행으로 센다)
     */
    public List<Integer> skippedRows() {
        return List.copyOf(skippedRows);
    }

    /**
     * 파일에서 규칙을 읽는다. 파일이 없으면 빈 상태로 시작한다.
     *
     * @throws IOException 파일을 읽지 못한 경우
     */
    public void load() throws IOException {
        items.clear();
        skippedRows.clear();
        List<String[]> rows = CsvStore.read(file);
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            try {
                int id = Integer.parseInt(row[0]);
                YearMonth last = row[6].isBlank() ? null : YearMonth.parse(row[6]);
                items.add(new RecurringRule(id, Integer.parseInt(row[1]), Long.parseLong(row[2]),
                        Integer.parseInt(row[3]), row[4], Boolean.parseBoolean(row[5]), last));
                nextId = Math.max(nextId, id + 1);
            } catch (RuntimeException e) {
                // 손상된 행은 건너뛰고 나머지는 정상 처리한다. 행 번호는 헤더를 감안해 +2.
                skippedRows.add(i + 2);
            }
        }
    }

    /**
     * 모든 규칙을 파일에 쓴다.
     *
     * @throws IOException 파일을 쓰지 못한 경우
     */
    public void save() throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (RecurringRule r : items) {
            rows.add(new String[] {
                String.valueOf(r.id()),
                String.valueOf(r.categoryId()),
                String.valueOf(r.amount()),
                String.valueOf(r.dayOfMonth()),
                r.memo(),
                String.valueOf(r.active()),
                r.lastGenerated() == null ? "" : r.lastGenerated().toString()
            });
        }
        CsvStore.write(file, HEADER, rows);
    }
}
