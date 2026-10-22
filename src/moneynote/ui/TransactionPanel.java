package moneynote.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalDate;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import moneynote.model.Category;
import moneynote.model.Transaction;
import moneynote.model.TransactionType;
import moneynote.service.LedgerService;
import moneynote.service.RecurringService;
import moneynote.service.TransactionFilter;
import moneynote.util.DateUtil;
import moneynote.util.MoneyFormat;

/** 거래 목록 화면. 조회, 필터, 등록, 수정, 삭제와 분류·고정 지출 관리 창을 모은다. (F1~F5, F9) */
public class TransactionPanel extends JPanel {

    private static final Color EXPENSE_COLOR = new Color(0xC0392B);
    private static final Color INCOME_COLOR = new Color(0x1F6FB2);

    private final LedgerService ledger;
    private final RecurringService recurring;
    private final TransactionTableModel model;
    private final JTable table;

    private final JTextField fromField = new JTextField(10);
    private final JTextField toField = new JTextField(10);
    private final JComboBox<String> typeBox = new RoomyComboBox<>(new String[] {"전체", "지출", "수입"});
    private final JComboBox<Object> categoryBox = new RoomyComboBox<>();
    private final JTextField keywordField = new JTextField(10);
    private final JLabel statusLabel = new JLabel(" ");

    /** 목록이 바뀌면 통계와 예산 화면도 다시 그려야 하므로 알림을 보낸다. */
    private Runnable onDataChanged = () -> { };

    /**
     * 거래내역 탭을 만든다. 이번 달 거래로 시작한다.
     *
     * @param ledger    거래·분류 서비스
     * @param recurring 고정 지출 서비스. 고정 지출 창과 분류 삭제 확인에 쓴다.
     */
    public TransactionPanel(LedgerService ledger, RecurringService recurring) {
        this.ledger = ledger;
        this.recurring = recurring;
        this.model = new TransactionTableModel(ledger.categories());
        this.table = new JTable(model);

        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        configureTable();
        selectThisMonth();
        refresh();
    }

    /**
     * 거래나 분류가 바뀌었을 때 부를 동작을 정한다. 메인 창이 통계와 예산 탭을 갱신하는 데 쓴다.
     *
     * @param listener 바뀐 뒤 부를 동작
     */
    public void setOnDataChanged(Runnable listener) {
        this.onDataChanged = listener;
    }

    private JPanel buildToolbar() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton add = new JButton("등록");
        JButton edit = new JButton("수정");
        JButton delete = new JButton("삭제");
        add.addActionListener(e -> openDialog(null));
        edit.addActionListener(e -> editSelected());
        delete.addActionListener(e -> deleteSelected());
        actions.add(add);
        actions.add(edit);
        actions.add(delete);

        JButton categories = new JButton("분류 관리");
        JButton recurringRules = new JButton("고정 지출");
        categories.addActionListener(e -> openCategoryDialog());
        recurringRules.addActionListener(e -> openRecurringDialog());
        actions.add(categories);
        actions.add(recurringRules);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton thisMonth = new JButton("이번 달");
        JButton lastMonth = new JButton("지난 달");
        JButton thisYear = new JButton("올해");
        JButton apply = new JButton("조회");
        thisMonth.addActionListener(e -> { selectThisMonth(); refresh(); });
        lastMonth.addActionListener(e -> { selectLastMonth(); refresh(); });
        thisYear.addActionListener(e -> { selectThisYear(); refresh(); });
        apply.addActionListener(e -> refresh());

        filters.add(new JLabel("기간"));
        filters.add(fromField);
        filters.add(new JLabel("~"));
        filters.add(toField);
        filters.add(thisMonth);
        filters.add(lastMonth);
        filters.add(thisYear);
        filters.add(new JLabel("유형"));
        filters.add(typeBox);
        filters.add(new JLabel("분류"));
        filters.add(categoryBox);
        filters.add(new JLabel("메모"));
        filters.add(keywordField);
        filters.add(apply);

        reloadCategoryFilter();

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(actions, BorderLayout.NORTH);
        top.add(filters, BorderLayout.SOUTH);
        return top;
    }

    private void configureTable() {
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 지출과 수입을 색으로 구분해 한눈에 읽히게 한다.
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focused, row, column);
                Transaction tx = model.at(t.convertRowIndexToModel(row));
                if (!selected) {
                    c.setForeground(tx.type() == TransactionType.EXPENSE ? EXPENSE_COLOR : INCOME_COLOR);
                }
                setHorizontalAlignment(column == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    /** 분류 필터 목록을 다시 채운다. 첫 항목은 전체를 뜻한다. */
    private void reloadCategoryFilter() {
        Object selected = categoryBox.getSelectedItem();
        categoryBox.removeAllItems();
        categoryBox.addItem("전체");
        for (Category c : ledger.categories().findAll()) {
            categoryBox.addItem(c);
        }
        if (selected != null) {
            categoryBox.setSelectedItem(selected);
        }
    }

    private void selectThisMonth() {
        LocalDate today = LocalDate.now();
        fromField.setText(DateUtil.format(today.withDayOfMonth(1)));
        toField.setText(DateUtil.format(today.withDayOfMonth(today.lengthOfMonth())));
    }

    private void selectLastMonth() {
        LocalDate base = LocalDate.now().minusMonths(1);
        fromField.setText(DateUtil.format(base.withDayOfMonth(1)));
        toField.setText(DateUtil.format(base.withDayOfMonth(base.lengthOfMonth())));
    }

    private void selectThisYear() {
        int year = LocalDate.now().getYear();
        fromField.setText(DateUtil.format(LocalDate.of(year, 1, 1)));
        toField.setText(DateUtil.format(LocalDate.of(year, 12, 31)));
    }

    /** 필터 조건을 읽어 목록과 요약 줄을 다시 그린다. */
    public void refresh() {
        TransactionFilter filter;
        try {
            filter = buildFilter();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "입력 확인",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Transaction> found = ledger.find(filter);
        model.setRows(found);
        updateStatus(found);
    }

    private TransactionFilter buildFilter() {
        LocalDate from = fromField.getText().isBlank() ? null : DateUtil.parse(fromField.getText());
        LocalDate to = toField.getText().isBlank() ? null : DateUtil.parse(toField.getText());
        if (from != null && to != null && from.isAfter(to)) {
            JOptionPane.showMessageDialog(this,
                    "시작일이 종료일보다 늦어 두 값을 바꿔 조회합니다.", "기간 확인",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        TransactionType type = switch (typeBox.getSelectedIndex()) {
            case 1 -> TransactionType.EXPENSE;
            case 2 -> TransactionType.INCOME;
            default -> null;
        };
        Object selectedCategory = categoryBox.getSelectedItem();
        Integer categoryId = selectedCategory instanceof Category c ? c.id() : null;

        return TransactionFilter.all()
                .period(from, to)
                .type(type)
                .category(categoryId)
                .keyword(keywordField.getText());
    }

    private void updateStatus(List<Transaction> found) {
        if (found.isEmpty()) {
            statusLabel.setText("조건에 맞는 거래가 없습니다.");
            return;
        }
        long income = found.stream().filter(t -> t.type() == TransactionType.INCOME)
                .mapToLong(Transaction::amount).sum();
        long expense = found.stream().filter(t -> t.type() == TransactionType.EXPENSE)
                .mapToLong(Transaction::amount).sum();
        statusLabel.setText(found.size() + "건    수입 " + MoneyFormat.display(income)
                + "    지출 " + MoneyFormat.display(expense)
                + "    잔액 " + MoneyFormat.signed(income - expense));
    }

    private void openDialog(Transaction editing) {
        TransactionDialog dialog = new TransactionDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this), ledger, editing);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refresh();
            onDataChanged.run();
        }
    }

    private void openCategoryDialog() {
        CategoryDialog dialog = new CategoryDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this), ledger, recurring);
        dialog.setVisible(true);
        if (dialog.isChanged()) {
            // 바뀐 분류 이름이 필터, 거래 목록, 통계, 예산에 모두 보여야 한다.
            reloadCategoryFilter();
            refresh();
            onDataChanged.run();
        }
    }

    /** 규칙은 다음 시작부터 거래를 만들므로 닫은 뒤 다시 그릴 것이 없다. */
    private void openRecurringDialog() {
        new RecurringDialog(javax.swing.SwingUtilities.getWindowAncestor(this), recurring,
                ledger.categories()).setVisible(true);
    }

    private void editSelected() {
        Transaction selected = selectedTransaction();
        if (selected == null) {
            return;
        }
        openDialog(selected);
    }

    private void deleteSelected() {
        Transaction selected = selectedTransaction();
        if (selected == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "이 거래를 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        ledger.delete(selected.id());
        refresh();
        onDataChanged.run();
    }

    private Transaction selectedTransaction() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "먼저 목록에서 거래를 선택하세요.", "안내",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return model.at(table.convertRowIndexToModel(row));
    }
}
