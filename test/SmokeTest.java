import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import moneynote.model.Category;
import moneynote.model.RecurringRule;
import moneynote.model.Transaction;
import moneynote.model.TransactionType;
import moneynote.repository.BudgetRepository;
import moneynote.repository.CategoryRepository;
import moneynote.repository.RecurringRepository;
import moneynote.repository.TransactionRepository;
import moneynote.service.BudgetService;
import moneynote.service.BudgetStatus;
import moneynote.service.LedgerService;
import moneynote.service.MonthlySummary;
import moneynote.service.RecurringService;
import moneynote.service.StatisticsService;
import moneynote.service.TransactionFilter;
import moneynote.util.DateUtil;
import moneynote.util.MoneyFormat;

/**
 * 서비스 계층 자체 검증 하네스.
 *
 * <p>JUnit을 쓰지 않는 환경이라 순수 Java로 작성했다. 화면 없이 로직만 확인하기 위한 것이며,
 * 실패한 항목이 하나라도 있으면 0이 아닌 종료 코드로 끝난다.
 */
public class SmokeTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("moneynote-smoke");

        testAmountParsing();
        testDayInMonthFallback();
        testFilterCombination(dir);
        testMonthlySummary(dir);
        testBudgetThresholds();
        testCsvRoundTrip(dir);
        testCategoryManagement(dir.resolve("category"));
        testRecurringGeneration(dir.resolve("recurring"));

        System.out.println();
        System.out.println("통과 " + passed + "건, 실패 " + failed + "건");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** MoneyFormat: 콤마 제거와 잘못된 입력 거부. */
    private static void testAmountParsing() {
        check("금액 파싱: 콤마 제거", MoneyFormat.parse("12,500") == 12500L);
        check("금액 파싱: 단위 포함", MoneyFormat.parse(" 3,000원 ") == 3000L);
        check("금액 파싱: 문자 거부", throwsIae(() -> MoneyFormat.parse("천원")));
        check("금액 파싱: 0 거부", throwsIae(() -> MoneyFormat.parse("0")));
    }

    /** DateUtil: 그 달에 없는 날짜는 마지막 날로 대체한다. (F9 예외 규칙) */
    private static void testDayInMonthFallback() {
        check("2026-02의 31일 -> 2월 28일",
                DateUtil.dayInMonth(YearMonth.of(2026, 2), 31).equals(LocalDate.of(2026, 2, 28)));
        check("2024-02의 31일 -> 윤년 29일",
                DateUtil.dayInMonth(YearMonth.of(2024, 2), 31).equals(LocalDate.of(2024, 2, 29)));
        check("2026-03의 10일 -> 그대로",
                DateUtil.dayInMonth(YearMonth.of(2026, 3), 10).equals(LocalDate.of(2026, 3, 10)));
    }

    /** LedgerService: 기간, 유형, 분류, 키워드 조건이 AND로 결합되는지. (F4) */
    private static void testFilterCombination(Path dir) throws Exception {
        Fixture f = new Fixture(dir.resolve("filter"));
        int food = f.expenseCategory("식비");
        int transport = f.expenseCategory("교통");
        int salary = f.incomeCategory("급여");

        f.ledger.register(LocalDate.of(2026, 9, 1), TransactionType.EXPENSE, 12000, food, "카드", "점심 김치찌개");
        f.ledger.register(LocalDate.of(2026, 9, 5), TransactionType.EXPENSE, 1500, transport, "카드", "버스");
        f.ledger.register(LocalDate.of(2026, 8, 20), TransactionType.EXPENSE, 9000, food, "현금", "점심 국밥");
        f.ledger.register(LocalDate.of(2026, 9, 25), TransactionType.INCOME, 500000, salary, "계좌이체", "월급");

        List<Transaction> all = f.ledger.find(TransactionFilter.all());
        check("필터 없음: 전체 4건", all.size() == 4);
        check("정렬: 날짜 내림차순", all.get(0).date().equals(LocalDate.of(2026, 9, 25)));

        List<Transaction> sept = f.ledger.find(TransactionFilter.all()
                .period(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));
        check("기간 필터: 9월 3건", sept.size() == 3);

        List<Transaction> septExpense = f.ledger.find(TransactionFilter.all()
                .period(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
                .type(TransactionType.EXPENSE));
        check("기간+유형: 9월 지출 2건", septExpense.size() == 2);

        List<Transaction> septFoodLunch = f.ledger.find(TransactionFilter.all()
                .period(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
                .type(TransactionType.EXPENSE)
                .category(food)
                .keyword("점심"));
        check("네 조건 AND: 1건", septFoodLunch.size() == 1);
        check("네 조건 AND: 금액 확인", septFoodLunch.get(0).amount() == 12000L);

        TransactionFilter swapped = TransactionFilter.all()
                .period(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1));
        check("기간 역전: 자동 교정", swapped.from().equals(LocalDate.of(2026, 9, 1)));

        check("검증: 유형 불일치 분류 거부", throwsIae(() ->
                f.ledger.register(LocalDate.of(2026, 9, 1), TransactionType.EXPENSE, 1000, salary, "카드", "")));
        check("검증: 음수 금액 거부", throwsIae(() ->
                f.ledger.register(LocalDate.of(2026, 9, 1), TransactionType.EXPENSE, -1, food, "카드", "")));
    }

    /** StatisticsService: 월 합계, 잔액, 전월 대비 증감. (F6) */
    private static void testMonthlySummary(Path dir) throws Exception {
        Fixture f = new Fixture(dir.resolve("summary"));
        int food = f.expenseCategory("식비");
        int salary = f.incomeCategory("급여");

        f.ledger.register(LocalDate.of(2026, 8, 10), TransactionType.EXPENSE, 100000, food, "카드", "");
        f.ledger.register(LocalDate.of(2026, 9, 10), TransactionType.EXPENSE, 120000, food, "카드", "");
        f.ledger.register(LocalDate.of(2026, 9, 25), TransactionType.INCOME, 500000, salary, "계좌이체", "");

        MonthlySummary sep = f.statistics.summarize(YearMonth.of(2026, 9));
        check("월 요약: 총수입", sep.income() == 500000L);
        check("월 요약: 총지출", sep.expense() == 120000L);
        check("월 요약: 잔액", sep.balance() == 380000L);
        check("월 요약: 전월 대비 증감액", sep.expenseDelta() == 20000L);
        check("월 요약: 증감률 20%", Math.abs(sep.expenseRate() - 20.0) < 0.001);

        MonthlySummary aug = f.statistics.summarize(YearMonth.of(2026, 8));
        check("전월 데이터 없음: 증감률 계산 안 함", !aug.hasExpenseRate());

        check("분류별 비중: 식비 100%",
                f.statistics.expenseByCategory(YearMonth.of(2026, 9)).get(0).percent() == 100.0);
        check("지출 0원인 달: 빈 목록",
                f.statistics.expenseByCategory(YearMonth.of(2026, 7)).isEmpty());
    }

    /** BudgetService: 80% / 100% 경계 판정. (F8) */
    private static void testBudgetThresholds() {
        check("79.9% -> 정상", BudgetService.levelOf(79_900, 100_000) == BudgetStatus.Level.NORMAL);
        check("80% 경계 -> 주의", BudgetService.levelOf(80_000, 100_000) == BudgetStatus.Level.WARNING);
        check("99.9% -> 주의", BudgetService.levelOf(99_900, 100_000) == BudgetStatus.Level.WARNING);
        check("100% 경계 -> 초과", BudgetService.levelOf(100_000, 100_000) == BudgetStatus.Level.EXCEEDED);
        check("한도 없음 -> 미설정", BudgetService.levelOf(50_000, 0) == BudgetStatus.Level.UNSET);
    }

    /** 저장 후 다시 읽어 데이터가 그대로인지. 메모의 쉼표와 따옴표 포함. (F10) */
    private static void testCsvRoundTrip(Path dir) throws Exception {
        Path base = dir.resolve("csv");
        Fixture f = new Fixture(base);
        int food = f.expenseCategory("식비");
        String tricky = "커피, 케이크 그리고 \"디저트\"";
        f.ledger.register(LocalDate.of(2026, 9, 8), TransactionType.EXPENSE, 8500, food, "카드", tricky);
        f.ledger.save();

        TransactionRepository reloaded = new TransactionRepository(base.resolve("transactions.csv"));
        reloaded.load();
        check("CSV 왕복: 1건 복원", reloaded.findAll().size() == 1);
        Transaction t = reloaded.findAll().get(0);
        check("CSV 왕복: 금액 보존", t.amount() == 8500L);
        check("CSV 왕복: 날짜 보존", t.date().equals(LocalDate.of(2026, 9, 8)));
        check("CSV 왕복: 쉼표와 따옴표가 든 메모 보존", tricky.equals(t.memo()));
        check("CSV 왕복: 손상된 행 없음", reloaded.skippedRows().isEmpty());
    }

    /** 저장소와 서비스를 한 벌 묶어 만든 테스트용 환경. */
    /** 분류 추가·이름 변경·삭제 규칙. (F5, UC5~UC7) */
    private static void testCategoryManagement(Path dir) throws Exception {
        Fixture f = new Fixture(dir);
        int food = f.expenseCategory("식비");
        int transport = f.expenseCategory("교통");
        int salary = f.incomeCategory("급여");

        f.ledger.renameCategory(food, "  외식  ");
        check("이름 변경: 공백 제거", f.categories.nameOf(food).equals("외식"));
        f.ledger.renameCategory(food, "외식");
        check("이름 변경: 자기 이름 그대로는 허용", f.categories.nameOf(food).equals("외식"));
        check("이름 변경: 같은 유형 중복 거부",
                throwsIae(() -> f.ledger.renameCategory(food, "교통")));
        int ott = f.ledger.addCategory("ott", TransactionType.EXPENSE).id();
        check("분류 추가: 대소문자만 다른 중복 거부",
                throwsIae(() -> f.ledger.addCategory("OTT", TransactionType.EXPENSE)));
        check("이름 변경: 대소문자만 다른 중복 거부",
                throwsIae(() -> f.ledger.renameCategory(transport, "OTT")));
        f.ledger.deleteCategory(ott);
        check("이름 변경: 빈 이름 거부", throwsIae(() -> f.ledger.renameCategory(food, "  ")));
        f.ledger.renameCategory(salary, "교통");
        check("이름 변경: 다른 유형이면 같은 이름 허용", f.categories.nameOf(salary).equals("교통"));

        f.ledger.register(LocalDate.of(2026, 10, 3), TransactionType.EXPENSE, 1500, transport,
                "카드", "버스");
        check("사용 건수: 1건", f.ledger.usageCount(transport) == 1);
        check("분류 삭제: 사용 중이면 거부", throwsIse(() -> f.ledger.deleteCategory(transport)));
        f.ledger.deleteCategory(food);
        check("분류 삭제: 미사용이면 삭제", f.categories.findById(food).isEmpty());
    }

    /** 고정 지출 자동 생성: 한 달에 한 번, 지운 거래는 되살리지 않음, 저장 후에도 유지. (F9, UC12, UC13) */
    private static void testRecurringGeneration(Path dir) throws Exception {
        Fixture f = new Fixture(dir);
        int housing = f.expenseCategory("주거");
        int culture = f.expenseCategory("문화");
        int salary = f.incomeCategory("급여");
        RecurringRepository repo = new RecurringRepository(dir.resolve("recurring.csv"));
        RecurringService recurring = new RecurringService(repo, f.ledger);

        check("규칙 검증: 금액 0 거부", throwsIae(() -> recurring.addRule(housing, 0, 1, "월세")));
        check("규칙 검증: 결제일 32 거부", throwsIae(() -> recurring.addRule(housing, 1000, 32, "")));
        check("규칙 검증: 수입 분류 거부", throwsIae(() -> recurring.addRule(salary, 1000, 1, "")));

        RecurringRule rent = recurring.addRule(housing, 450_000, 31, "월세");
        RecurringRule music = recurring.addRule(culture, 17_000, 5, "음악, \"구독\"");
        recurring.setActive(music.id(), false);

        YearMonth feb = YearMonth.of(2026, 2);
        List<Transaction> created = recurring.generateFor(feb);
        check("자동 생성: 활성 규칙만 1건", created.size() == 1);
        check("자동 생성: 31일 -> 2월 말일", created.get(0).date().equals(LocalDate.of(2026, 2, 28)));
        check("자동 생성: 자동 표시", created.get(0).autoGenerated());
        check("자동 생성: 같은 달 재실행 시 0건", recurring.generateFor(feb).isEmpty());

        f.ledger.delete(created.get(0).id());
        check("자동 생성: 지운 거래를 되살리지 않음", recurring.generateFor(feb).isEmpty());

        f.ledger.save();
        recurring.save();
        RecurringRepository reloaded = new RecurringRepository(dir.resolve("recurring.csv"));
        reloaded.load();
        RecurringService again = new RecurringService(reloaded, f.ledger);
        check("규칙 CSV 왕복: 2개 복원", reloaded.findAll().size() == 2);
        check("규칙 CSV 왕복: 마지막 생성 월 보존",
                feb.equals(reloaded.findById(rent.id()).orElseThrow().lastGenerated()));
        check("규칙 CSV 왕복: 비활성·메모 보존",
                !reloaded.findById(music.id()).orElseThrow().active()
                        && "음악, \"구독\"".equals(reloaded.findById(music.id()).orElseThrow().memo()));
        check("재시작: 같은 달 0건", again.generateFor(feb).isEmpty());
        check("재시작: 다음 달 1건", again.generateFor(feb.plusMonths(1)).size() == 1);

        again.deleteRule(music.id());
        check("규칙 삭제: 1개 남음", again.rules().size() == 1);

        // 분류가 사라진 규칙이 있어도 시작이 막히지 않고 나머지 규칙은 만들어진다.
        int temp = f.expenseCategory("임시");
        RecurringRule orphan = again.addRule(temp, 500, 1, "임시 규칙");
        f.ledger.deleteCategory(temp);
        List<Transaction> april = again.generateFor(YearMonth.of(2026, 4));
        check("분류 없는 규칙: 건너뛰고 1건만 생성",
                april.size() == 1 && april.get(0).memo().equals("월세"));
        check("분류 없는 규칙: 생성 기록 남기지 않음", orphan.lastGenerated() == null);
    }

    private static class Fixture {
        final CategoryRepository categories;
        final TransactionRepository transactions;
        final LedgerService ledger;
        final StatisticsService statistics;
        final BudgetService budgets;

        Fixture(Path base) throws Exception {
            Files.createDirectories(base);
            categories = new CategoryRepository(base.resolve("categories.csv"));
            transactions = new TransactionRepository(base.resolve("transactions.csv"));
            categories.load();
            transactions.load();
            ledger = new LedgerService(transactions, categories);
            statistics = new StatisticsService(transactions, categories);
            budgets = new BudgetService(new BudgetRepository(base.resolve("budgets.csv")),
                    categories, statistics);
        }

        int expenseCategory(String name) {
            return findOrAdd(name, TransactionType.EXPENSE);
        }

        int incomeCategory(String name) {
            return findOrAdd(name, TransactionType.INCOME);
        }

        private int findOrAdd(String name, TransactionType type) {
            for (Category c : categories.findByType(type)) {
                if (c.name().equals(name)) {
                    return c.id();
                }
            }
            return ledger.addCategory(name, type).id();
        }
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + label);
        } else {
            failed++;
            System.out.println("  FAIL  " + label);
        }
    }

    private static boolean throwsIae(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean throwsIse(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
