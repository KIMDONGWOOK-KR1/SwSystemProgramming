package moneynote.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import moneynote.model.Budget;
import moneynote.model.Category;
import moneynote.model.TransactionType;
import moneynote.repository.BudgetRepository;
import moneynote.repository.CategoryRepository;

/**
 * 예산 대비 소비를 판정한다. (F8)
 *
 * <p>기록만 하는 가계부와 달리 프로그램이 먼저 경고를 내놓는 부분이라, 임계값 판정 규칙을
 * 화면이 아닌 이 계층에 둔다.
 */
public class BudgetService {

    /** 주의 단계로 넘어가는 사용률(%). */
    public static final double WARNING_THRESHOLD = 80.0;
    /** 초과 단계로 넘어가는 사용률(%). */
    public static final double EXCEEDED_THRESHOLD = 100.0;

    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final StatisticsService statistics;

    /**
     * 서비스를 만든다.
     *
     * @param budgets    예산 저장소
     * @param categories 분류 이름과 지출 분류 목록을 얻는 저장소
     * @param statistics 분류별 사용액을 계산하는 서비스
     */
    public BudgetService(BudgetRepository budgets, CategoryRepository categories,
                         StatisticsService statistics) {
        this.budgets = budgets;
        this.categories = categories;
        this.statistics = statistics;
    }

    /**
     * 분류의 월 예산 한도를 정한다. 같은 분류·같은 달이면 한도만 바꾼다. (UC10)
     *
     * @param categoryId  지출 분류 id
     * @param month       적용할 달
     * @param limitAmount 한도(원), 0보다 커야 한다
     * @throws IllegalArgumentException 한도가 0 이하인 경우
     */
    public void setLimit(int categoryId, YearMonth month, long limitAmount) {
        if (limitAmount <= 0) {
            throw new IllegalArgumentException("예산 한도는 0보다 커야 합니다.");
        }
        budgets.setLimit(categoryId, month, limitAmount);
    }

    /**
     * 지출 분류 전체의 예산 현황. 예산이 없는 분류는 UNSET으로 표시한다. (UC11)
     *
     * @param month 판정할 달
     * @return 지출 분류 순서대로 분류마다 하나씩의 현황
     */
    public List<BudgetStatus> statuses(YearMonth month) {
        List<BudgetStatus> result = new ArrayList<>();
        for (Category category : categories.findByType(TransactionType.EXPENSE)) {
            result.add(statusOf(category.id(), month));
        }
        return result;
    }

    /**
     * 한 분류의 예산 현황을 판정한다.
     *
     * @param categoryId 지출 분류 id
     * @param month      판정할 달
     * @return 사용액, 한도, 판정 단계를 담은 현황
     */
    public BudgetStatus statusOf(int categoryId, YearMonth month) {
        String name = categories.nameOf(categoryId);
        long used = statistics.expenseOf(categoryId, month);
        Optional<Budget> budget = budgets.find(categoryId, month);
        if (budget.isEmpty()) {
            return new BudgetStatus(categoryId, name, used, 0L, BudgetStatus.Level.UNSET);
        }
        long limit = budget.get().limitAmount();
        return new BudgetStatus(categoryId, name, used, limit, levelOf(used, limit));
    }

    /**
     * 사용률로 단계를 판정한다.
     * 80% 미만은 정상, 80% 이상 100% 미만은 주의, 100% 이상은 초과.
     *
     * @param used  사용액(원)
     * @param limit 한도(원), 0 이하이면 예산이 없는 것으로 본다
     * @return 판정 단계, 한도가 없으면 {@link BudgetStatus.Level#UNSET}
     */
    public static BudgetStatus.Level levelOf(long used, long limit) {
        if (limit <= 0) {
            return BudgetStatus.Level.UNSET;
        }
        double rate = used * 100.0 / limit;
        if (rate >= EXCEEDED_THRESHOLD) {
            return BudgetStatus.Level.EXCEEDED;
        }
        if (rate >= WARNING_THRESHOLD) {
            return BudgetStatus.Level.WARNING;
        }
        return BudgetStatus.Level.NORMAL;
    }

    /**
     * 예산 저장소. 종료할 때 저장하는 데 쓴다.
     *
     * @return 예산 저장소. 종료할 때 저장하는 데 쓴다.
     */
    public BudgetRepository repository() {
        return budgets;
    }
}
