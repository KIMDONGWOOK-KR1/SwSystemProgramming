package moneynote.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import moneynote.service.CategoryShare;
import moneynote.service.MonthlySummary;
import moneynote.service.StatisticsService;
import moneynote.util.MoneyFormat;

/** 월별 수지 요약과 분류별 지출 통계. (F6, F7) */
public class StatisticsPanel extends JPanel {

    private static final Color NEGATIVE = new Color(0xC0392B);

    private final StatisticsService statistics;

    private final JTextField monthField = new JTextField(8);
    private final JLabel incomeLabel = new JLabel();
    private final JLabel expenseLabel = new JLabel();
    private final JLabel balanceLabel = new JLabel();
    private final JLabel deltaLabel = new JLabel();
    private final DefaultTableModel shareModel =
            new DefaultTableModel(new String[] {"분류", "금액", "비중"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final BarChartPanel chart = new BarChartPanel();

    /**
     * 통계 탭을 만든다. 이번 달로 시작한다.
     *
     * @param statistics 통계 서비스
     */
    public StatisticsPanel(StatisticsService statistics) {
        this.statistics = statistics;

        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(new JTable(shareModel)), chart);
        split.setResizeWeight(0.4);
        add(split, BorderLayout.CENTER);

        monthField.setText(YearMonth.now().toString());
        refresh();
    }

    private JPanel buildHeader() {
        JButton apply = new JButton("조회");
        apply.addActionListener(e -> refresh());

        JPanel selector = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        selector.add(new JLabel("조회 월 (yyyy-MM)"));
        selector.add(monthField);
        selector.add(apply);

        JPanel summary = new JPanel(new GridLayout(1, 4, 12, 0));
        summary.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        summary.add(labeled("총수입", incomeLabel));
        summary.add(labeled("총지출", expenseLabel));
        summary.add(labeled("잔액", balanceLabel));
        summary.add(labeled("전월 대비 지출", deltaLabel));

        JPanel header = new JPanel(new BorderLayout());
        header.add(selector, BorderLayout.NORTH);
        header.add(summary, BorderLayout.SOUTH);
        return header;
    }

    private JPanel labeled(String title, JLabel valueLabel) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.GRAY);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 16f));

        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.add(titleLabel);
        panel.add(valueLabel);
        return panel;
    }

    /** 거래가 바뀌면 MainFrame이 이 메서드를 불러 화면을 다시 그린다. */
    public void refresh() {
        YearMonth month;
        try {
            month = YearMonth.parse(monthField.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "조회 월은 yyyy-MM 형식으로 입력하세요.",
                    "입력 확인", JOptionPane.WARNING_MESSAGE);
            return;
        }

        MonthlySummary summary = statistics.summarize(month);
        incomeLabel.setText(MoneyFormat.display(summary.income()));
        expenseLabel.setText(MoneyFormat.display(summary.expense()));
        balanceLabel.setText(MoneyFormat.display(summary.balance()));
        balanceLabel.setForeground(summary.balance() < 0 ? NEGATIVE : Color.BLACK);
        deltaLabel.setText(describeDelta(summary));

        List<CategoryShare> shares = statistics.expenseByCategory(month);
        shareModel.setRowCount(0);
        for (CategoryShare share : shares) {
            shareModel.addRow(new Object[] {
                share.categoryName(),
                MoneyFormat.display(share.amount()),
                String.format("%.1f%%", share.percent())
            });
        }
        chart.setShares(shares);
    }

    private String describeDelta(MonthlySummary summary) {
        if (!summary.hasExpenseRate()) {
            return "비교 데이터 없음";
        }
        String arrow = summary.expenseDelta() >= 0 ? "▲" : "▼";
        return arrow + " " + String.format("%.1f%%", Math.abs(summary.expenseRate()))
                + " (" + MoneyFormat.signed(summary.expenseDelta()) + ")";
    }
}
