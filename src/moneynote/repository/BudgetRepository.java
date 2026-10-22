package moneynote.repository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import moneynote.model.Budget;

/** 분류별 월 예산 저장소. (F8) */
public class BudgetRepository {

    private static final String[] HEADER = {"categoryId", "month", "limitAmount"};

    private final Path file;
    private final List<Budget> items = new ArrayList<>();

    /**
     * 저장소를 만든다. 파일은 {@link #load()} 를 부를 때 읽는다.
     *
     * @param file 저장할 CSV 파일 경로
     */
    public BudgetRepository(Path file) {
        this.file = file;
    }

    /**
     * 한 달의 예산 목록.
     *
     * @param month 달
     * @return 그 달에 정한 예산
     */
    public List<Budget> findByMonth(YearMonth month) {
        return items.stream().filter(b -> b.month().equals(month)).toList();
    }

    /**
     * 한 분류, 한 달의 예산.
     *
     * @param categoryId 분류 id
     * @param month      달
     * @return 예산, 없으면 빈 값
     */
    public Optional<Budget> find(int categoryId, YearMonth month) {
        return items.stream()
                .filter(b -> b.categoryId() == categoryId && b.month().equals(month))
                .findFirst();
    }

    /**
     * 같은 분류, 같은 달의 예산이 이미 있으면 한도만 바꾸고 없으면 새로 만든다.
     *
     * @param categoryId  분류 id
     * @param month       달
     * @param limitAmount 한도(원)
     * @return 바뀌었거나 새로 만든 예산
     */
    public Budget setLimit(int categoryId, YearMonth month, long limitAmount) {
        Optional<Budget> existing = find(categoryId, month);
        if (existing.isPresent()) {
            existing.get().setLimitAmount(limitAmount);
            return existing.get();
        }
        Budget budget = new Budget(categoryId, month, limitAmount);
        items.add(budget);
        return budget;
    }

    /**
     * 파일에서 읽는다. 파일이 없으면 빈 상태로 시작한다.
     *
     * @throws IOException 파일을 읽지 못한 경우
     */
    public void load() throws IOException {
        items.clear();
        for (String[] row : CsvStore.read(file)) {
            if (row.length < 3) {
                continue;
            }
            items.add(new Budget(Integer.parseInt(row[0]), YearMonth.parse(row[1]),
                    Long.parseLong(row[2])));
        }
    }

    /**
     * 모든 항목을 파일에 쓴다.
     *
     * @throws IOException 파일을 쓰지 못한 경우
     */
    public void save() throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (Budget b : items) {
            rows.add(new String[] {
                String.valueOf(b.categoryId()), b.month().toString(),
                String.valueOf(b.limitAmount())
            });
        }
        CsvStore.write(file, HEADER, rows);
    }
}
