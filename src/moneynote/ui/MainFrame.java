package moneynote.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import moneynote.repository.BudgetRepository;
import moneynote.service.BudgetService;
import moneynote.service.LedgerService;
import moneynote.service.RecurringService;
import moneynote.service.StatisticsService;

/** 메인 창. 거래내역, 통계, 예산 세 화면을 탭으로 묶는다. */
public class MainFrame extends JFrame {

    private final LedgerService ledger;
    private final BudgetRepository budgetRepository;
    private final RecurringService recurring;

    private final TransactionPanel transactionPanel;
    private final StatisticsPanel statisticsPanel;
    private final BudgetPanel budgetPanel;

    /**
     * 메인 창을 만든다. 창을 닫으면 모든 데이터를 저장하고 끝난다.
     *
     * @param ledger     거래·분류 서비스
     * @param statistics 통계 서비스
     * @param budgets    예산 서비스
     * @param recurring  고정 지출 서비스
     */
    public MainFrame(LedgerService ledger, StatisticsService statistics, BudgetService budgets,
                     RecurringService recurring) {
        super("MoneyNote - 개인 가계부");
        this.ledger = ledger;
        this.budgetRepository = budgets.repository();
        this.recurring = recurring;

        transactionPanel = new TransactionPanel(ledger, recurring);
        statisticsPanel = new StatisticsPanel(statistics);
        budgetPanel = new BudgetPanel(budgets, ledger.categories());

        // 거래가 바뀌면 집계와 예산 판정도 함께 갱신되어야 한다.
        transactionPanel.setOnDataChanged(() -> {
            statisticsPanel.refresh();
            budgetPanel.refresh();
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("거래내역", transactionPanel);
        tabs.addTab("통계", statisticsPanel);
        tabs.addTab("예산", budgetPanel);

        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveAndExit();
            }
        });

        setMinimumSize(new Dimension(880, 560));
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * 종료 시 데이터를 파일에 기록한다. 쓰기에 실패하면 종료를 보류해 유실을 막는다. (F10)
     */
    private void saveAndExit() {
        try {
            ledger.save();
            budgetRepository.save();
            recurring.save();
            dispose();
        } catch (IOException e) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "데이터를 저장하지 못했습니다: " + e.getMessage()
                            + System.lineSeparator() + "저장하지 않고 종료할까요?",
                    "저장 실패", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                dispose();
            }
        }
    }
}
