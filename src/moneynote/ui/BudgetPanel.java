package moneynote.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import moneynote.model.Category;
import moneynote.model.TransactionType;
import moneynote.repository.CategoryRepository;
import moneynote.service.BudgetService;
import moneynote.service.BudgetStatus;
import moneynote.util.MoneyFormat;

/** 분류별 예산 설정과 사용률 현황. (F8) */
public class BudgetPanel extends JPanel {

    private static final Color NORMAL = new Color(0x2E7D32);
    private static final Color WARNING = new Color(0xB8860B);
    private static final Color EXCEEDED = new Color(0xC0392B);

    private final BudgetService budgets;
    private final CategoryRepository categories;

    private final JTextField monthField = new JTextField(8);
    private final JComboBox<Category> categoryBox = new RoomyComboBox<>();
    private final JTextField limitField = new JTextField(10);
    private final DefaultTableModel model =
            new DefaultTableModel(new String[] {"분류", "사용액", "한도", "사용률", "상태"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable table = new JTable(model);

    /**
     * 예산 탭을 만든다.
     *
     * @param budgets    예산 서비스
     * @param categories 지출 분류 목록을 채우는 데 쓰는 저장소
     */
    public BudgetPanel(BudgetService budgets, CategoryRepository categories) {
        this.budgets = budgets;
        this.categories = categories;

        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        configureTable();
        monthField.setText(YearMonth.now().toString());
        reloadCategories();
        refresh();
    }

    private JPanel buildHeader() {
        JButton apply = new JButton("조회");
        JButton save = new JButton("예산 설정");
        apply.addActionListener(e -> refresh());
        save.addActionListener(e -> saveLimit());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        header.add(new JLabel("조회 월 (yyyy-MM)"));
        header.add(monthField);
        header.add(apply);
        header.add(new JLabel("    분류"));
        header.add(categoryBox);
        header.add(new JLabel("한도"));
        header.add(limitField);
        header.add(save);
        return header;
    }

    private void configureTable() {
        table.setRowHeight(24);
        // 상태 열의 글자색으로 정상, 주의, 초과를 구분한다.
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focused, row, column);
                if (!selected) {
                    c.setForeground(colorOf(String.valueOf(value)));
                }
                return c;
            }
        });
    }

    /** 초과 행은 뒤에 초과 금액이 덧붙으므로 접두사로 판정한다. */
    private Color colorOf(String cellText) {
        if (cellText.startsWith(BudgetStatus.Level.EXCEEDED.label())) {
            return EXCEEDED;
        }
        if (cellText.startsWith(BudgetStatus.Level.WARNING.label())) {
            return WARNING;
        }
        if (cellText.startsWith(BudgetStatus.Level.NORMAL.label())) {
            return NORMAL;
        }
        return Color.GRAY;
    }

    private void reloadCategories() {
        categoryBox.removeAllItems();
        for (Category c : categories.findByType(TransactionType.EXPENSE)) {
            categoryBox.addItem(c);
        }
    }

    private void saveLimit() {
        Category category = (Category) categoryBox.getSelectedItem();
        if (category == null) {
            JOptionPane.showMessageDialog(this, "분류를 선택하세요.", "안내",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            YearMonth month = parseMonth();
            long limit = MoneyFormat.parse(limitField.getText());
            budgets.setLimit(category.id(), month, limit);
            limitField.setText("");
            refresh();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "입력 확인",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /** 거래가 바뀌면 MainFrame이 이 메서드를 불러 사용률을 다시 판정한다. */
    public void refresh() {
        YearMonth month;
        try {
            month = parseMonth();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "입력 확인",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        reloadCategories();

        List<BudgetStatus> statuses = budgets.statuses(month);
        model.setRowCount(0);
        for (BudgetStatus status : statuses) {
            boolean unset = status.level() == BudgetStatus.Level.UNSET;
            model.addRow(new Object[] {
                status.categoryName(),
                MoneyFormat.display(status.used()),
                unset ? "-" : MoneyFormat.display(status.limit()),
                unset ? "-" : String.format("%.1f%%", status.usageRate()),
                describe(status)
            });
        }
    }

    private String describe(BudgetStatus status) {
        if (status.level() == BudgetStatus.Level.EXCEEDED) {
            return status.level().label() + " (+" + MoneyFormat.display(status.overflow()) + ")";
        }
        return status.level().label();
    }

    private YearMonth parseMonth() {
        try {
            return YearMonth.parse(monthField.getText().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("조회 월은 yyyy-MM 형식으로 입력하세요.");
        }
    }
}
