package moneynote.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import moneynote.model.Transaction;
import moneynote.model.TransactionType;
import moneynote.repository.CategoryRepository;
import moneynote.repository.TransactionRepository;
import moneynote.util.DateUtil;

/** 집계 계산. 저장된 값이 아니라 계산된 값을 만들어내는 계층이다. (F6, F7) */
public class StatisticsService {

    /** 그래프에 개별 막대로 그릴 최대 분류 수. 나머지는 기타로 묶는다. */
    private static final int TOP_N = 8;

    private final TransactionRepository transactions;
    private final CategoryRepository categories;

    /**
     * 서비스를 만든다.
     *
     * @param transactions 거래 저장소
     * @param categories   분류 이름을 얻는 저장소
     */
    public StatisticsService(TransactionRepository transactions, CategoryRepository categories) {
        this.transactions = transactions;
        this.categories = categories;
    }

    /**
     * F6: 지정한 달의 수지 요약. 전월과 비교한 증감까지 함께 계산한다. (UC8)
     *
     * @param month 요약할 달
     * @return 총수입, 총지출, 잔액, 전월 대비 증감
     */
    public MonthlySummary summarize(YearMonth month) {
        long income = sum(month, TransactionType.INCOME);
        long expense = sum(month, TransactionType.EXPENSE);
        YearMonth previous = month.minusMonths(1);
        boolean hasPrevious = hasAnyTransaction(previous);
        long previousExpense = hasPrevious ? sum(previous, TransactionType.EXPENSE) : 0L;
        return new MonthlySummary(month, income, expense, previousExpense, hasPrevious);
    }

    /**
     * 한 달의 수입 또는 지출 합계.
     *
     * @param month 합계를 낼 달
     * @param type  수입 또는 지출
     * @return 합계(원)
     */
    public long sum(YearMonth month, TransactionType type) {
        return transactions.findAll().stream()
                .filter(t -> t.type() == type && DateUtil.isIn(t.date(), month))
                .mapToLong(Transaction::amount)
                .sum();
    }

    private boolean hasAnyTransaction(YearMonth month) {
        return transactions.findAll().stream().anyMatch(t -> DateUtil.isIn(t.date(), month));
    }

    /**
     * 특정 달, 특정 분류의 지출 합계. 예산 판정(F8)에서 쓴다.
     *
     * @param categoryId 분류 id
     * @param month      합계를 낼 달
     * @return 지출 합계(원)
     */
    public long expenseOf(int categoryId, YearMonth month) {
        return transactions.findAll().stream()
                .filter(t -> t.type() == TransactionType.EXPENSE)
                .filter(t -> t.categoryId() == categoryId)
                .filter(t -> DateUtil.isIn(t.date(), month))
                .mapToLong(Transaction::amount)
                .sum();
    }

    /**
     * F7: 분류별 지출 금액과 비중을 금액 내림차순으로 돌려준다. (UC9)
     * 분류가 많으면 상위 8개만 남기고 나머지는 기타로 합친다.
     *
     * @param month 집계할 달
     * @return 분류별 금액과 비중, 지출이 없으면 빈 목록
     */
    public List<CategoryShare> expenseByCategory(YearMonth month) {
        Map<Integer, Long> totals = new LinkedHashMap<>();
        for (Transaction t : transactions.findAll()) {
            if (t.type() != TransactionType.EXPENSE || !DateUtil.isIn(t.date(), month)) {
                continue;
            }
            totals.merge(t.categoryId(), t.amount(), Long::sum);
        }

        long grandTotal = totals.values().stream().mapToLong(Long::longValue).sum();
        if (grandTotal == 0) {
            return List.of();
        }

        List<Map.Entry<Integer, Long>> sorted = new ArrayList<>(totals.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        List<CategoryShare> result = new ArrayList<>();
        long others = 0L;
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<Integer, Long> entry = sorted.get(i);
            if (i < TOP_N) {
                result.add(new CategoryShare(
                        entry.getKey(),
                        categories.nameOf(entry.getKey()),
                        entry.getValue(),
                        entry.getValue() * 100.0 / grandTotal));
            } else {
                others += entry.getValue();
            }
        }
        if (others > 0) {
            result.add(new CategoryShare(-1, "기타", others, others * 100.0 / grandTotal));
        }
        return result;
    }
}
