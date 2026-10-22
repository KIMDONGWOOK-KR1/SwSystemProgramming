package moneynote.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import moneynote.model.Category;
import moneynote.model.RecurringRule;
import moneynote.model.TransactionType;
import moneynote.repository.CategoryRepository;
import moneynote.service.RecurringService;
import moneynote.util.MoneyFormat;

/**
 * 고정 지출 관리 창. 규칙을 추가하고, 켜고 끄고, 지운다. (F9, UC12)
 *
 * <p>규칙은 바로 거래를 만들지 않는다. 거래는 프로그램을 시작할 때
 * {@link RecurringService#generateFor} 가 달마다 한 번 만든다.
 */
public class RecurringDialog extends JDialog {

    private static final String[] COLUMNS = {"분류", "금액", "결제일", "메모", "활성", "마지막 생성"};

    private final RecurringService recurring;
    private final CategoryRepository categories;

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public Class<?> getColumnClass(int column) {
            return column == 4 ? Boolean.class : Object.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<Category> categoryBox = new RoomyComboBox<>();
    private final JTextField amountField = new JTextField(8);
    private final JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
    private final JTextField memoField = new JTextField(10);
    private final JCheckBox activeBox = new JCheckBox("활성", true);

    /** 표의 행 순서와 같은 순서의 규칙. */
    private List<RecurringRule> rows = List.of();

    /**
     * 고정 지출 관리 창을 만든다. 모달이다.
     *
     * @param owner      부모 창
     * @param recurring  규칙을 관리하는 서비스
     * @param categories 지출 분류 목록을 채우는 데 쓰는 저장소
     */
    public RecurringDialog(java.awt.Window owner, RecurringService recurring,
                           CategoryRepository categories) {
        super(owner, "고정 지출 관리", ModalityType.APPLICATION_MODAL);
        this.recurring = recurring;
        this.categories = categories;

        buildLayout();
        for (Category c : categories.findByType(TransactionType.EXPENSE)) {
            categoryBox.addItem(c);
        }
        reload();

        pack();
        setLocationRelativeTo(owner);
    }

    private void buildLayout() {
        table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 활성 체크 상자는 고른 규칙의 상태를 따라간다.
        table.getSelectionModel().addListSelectionListener(e -> {
            RecurringRule selected = selectedRule();
            if (!e.getValueIsAdjusting() && selected != null) {
                activeBox.setSelected(selected.active());
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(500, 130));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        form.add(new JLabel("분류"));
        form.add(categoryBox);
        form.add(new JLabel("금액"));
        form.add(amountField);
        form.add(new JLabel("결제일"));
        form.add(daySpinner);
        form.add(new JLabel("메모"));
        form.add(memoField);

        JButton add = new JButton("규칙 추가");
        JButton delete = new JButton("규칙 삭제");
        add.addActionListener(e -> addRule());
        activeBox.addActionListener(e -> toggleSelected());
        delete.addActionListener(e -> deleteSelected());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        buttons.add(add);
        buttons.add(activeBox);
        buttons.add(delete);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 6));
        center.add(form);
        center.add(buttons);

        JLabel hint = new JLabel("활성 규칙은 프로그램을 시작할 때 이번 달 거래로 한 번만 만들어집니다.");
        hint.setForeground(Color.GRAY);
        JButton close = new JButton("닫기");
        close.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(hint, BorderLayout.WEST);
        bottom.add(close, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(scroll, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);
        setContentPane(content);
    }

    private void reload() {
        rows = recurring.rules();
        model.setRowCount(0);
        for (RecurringRule r : rows) {
            model.addRow(new Object[] {
                categories.nameOf(r.categoryId()),
                MoneyFormat.plain(r.amount()),
                "매월 " + r.dayOfMonth() + "일",
                r.memo(),
                r.active(),
                r.lastGenerated() == null ? "-" : r.lastGenerated().toString()
            });
        }
    }

    private void addRule() {
        Category category = (Category) categoryBox.getSelectedItem();
        if (category == null) {
            warn("분류를 선택하세요.");
            return;
        }
        try {
            long amount = MoneyFormat.parse(amountField.getText());
            int day = (Integer) daySpinner.getValue();
            recurring.addRule(category.id(), amount, day, memoField.getText().trim());
        } catch (IllegalArgumentException e) {
            warn(e.getMessage());
            return;
        }
        amountField.setText("");
        memoField.setText("");
        reload();
    }

    private void toggleSelected() {
        RecurringRule selected = selectedRule();
        if (selected == null) {
            return;
        }
        int row = table.getSelectedRow();
        recurring.setActive(selected.id(), activeBox.isSelected());
        reload();
        table.setRowSelectionInterval(row, row);
    }

    private void deleteSelected() {
        RecurringRule selected = selectedRule();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "먼저 목록에서 규칙을 선택하세요.", "안내",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "이 규칙을 삭제하시겠습니까? 이미 만든 거래는 남습니다.", "삭제 확인",
                JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        recurring.deleteRule(selected.id());
        reload();
    }

    private RecurringRule selectedRule() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(row);
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 확인", JOptionPane.WARNING_MESSAGE);
    }
}
