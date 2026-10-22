package moneynote;

import java.awt.EventQueue;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import moneynote.repository.BudgetRepository;
import moneynote.repository.CategoryRepository;
import moneynote.repository.RecurringRepository;
import moneynote.repository.TransactionRepository;
import moneynote.service.BudgetService;
import moneynote.service.LedgerService;
import moneynote.service.RecurringService;
import moneynote.service.StatisticsService;
import moneynote.ui.MainFrame;

/**
 * 진입점. 저장소와 서비스를 만들어 화면에 넘긴다.
 *
 * <p>객체를 여기서 한 번에 조립해 두면 각 화면은 자신이 쓸 서비스만 받으면 되고,
 * 검증용 하네스는 화면 없이 같은 서비스를 그대로 만들어 쓸 수 있다.
 */
public class Main {

    /** 데이터 폴더를 따로 주지 않으면 실행한 폴더 아래의 data 폴더를 쓴다. */
    private static final Path DEFAULT_DATA_DIR = Paths.get("data");

    private Main() {
    }

    /**
     * 프로그램을 시작한다.
     *
     * @param args 비어 있으면 ./data 를 쓰고, 첫 인자가 있으면 그 경로를 데이터 폴더로 쓴다
     */
    public static void main(String[] args) {
        applySystemLookAndFeel();
        start(args.length > 0 ? Paths.get(args[0]) : DEFAULT_DATA_DIR);
    }

    /**
     * 데이터를 불러오고, 이번 달 고정 지출을 만든 뒤 메인 창을 띄운다. (UC14, UC13)
     *
     * @param dataDir CSV 파일이 있는 폴더. 없으면 종료할 때 만든다.
     */
    public static void start(Path dataDir) {
        CategoryRepository categories = new CategoryRepository(dataDir.resolve("categories.csv"));
        TransactionRepository transactions =
                new TransactionRepository(dataDir.resolve("transactions.csv"));
        BudgetRepository budgets = new BudgetRepository(dataDir.resolve("budgets.csv"));
        RecurringRepository rules = new RecurringRepository(dataDir.resolve("recurring.csv"));

        try {
            categories.load();
            transactions.load();
            budgets.load();
            rules.load();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "데이터를 읽지 못했습니다: " + e.getMessage() + System.lineSeparator()
                            + "빈 상태로 시작합니다.",
                    "불러오기 실패", JOptionPane.WARNING_MESSAGE);
        }
        warnAboutSkippedRows("transactions.csv", transactions.skippedRows());
        warnAboutSkippedRows("recurring.csv", rules.skippedRows());

        LedgerService ledger = new LedgerService(transactions, categories);
        StatisticsService statistics = new StatisticsService(transactions, categories);
        BudgetService budgetService = new BudgetService(budgets, categories, statistics);
        RecurringService recurring = new RecurringService(rules, ledger);

        // 메인 창을 띄우기 전에 이번 달 고정 지출을 만들어야 첫 화면에 바로 보인다.
        int created = recurring.generateFor(YearMonth.now()).size();

        EventQueue.invokeLater(() -> {
            MainFrame frame = new MainFrame(ledger, statistics, budgetService, recurring);
            frame.setVisible(true);
            if (created > 0) {
                JOptionPane.showMessageDialog(frame,
                        "이번 달 고정 지출 " + created + "건을 거래내역에 추가했습니다.",
                        "고정 지출", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    /** 형식이 깨져 읽지 못한 행이 있으면 조용히 넘기지 않고 알린다. */
    private static void warnAboutSkippedRows(String fileName, List<Integer> skipped) {
        if (skipped.isEmpty()) {
            return;
        }
        JOptionPane.showMessageDialog(null,
                fileName + " 에서 읽지 못한 행이 " + skipped.size() + "개 있습니다: " + skipped
                        + System.lineSeparator() + "나머지 데이터는 정상적으로 불러왔습니다.",
                "일부 데이터 손상", JOptionPane.WARNING_MESSAGE);
    }

    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 기본 모양으로 떨어져도 동작에는 지장이 없으므로 넘어간다.
        }
    }
}
