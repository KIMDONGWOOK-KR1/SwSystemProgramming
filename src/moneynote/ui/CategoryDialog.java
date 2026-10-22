package moneynote.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import moneynote.model.Category;
import moneynote.model.TransactionType;
import moneynote.service.LedgerService;
import moneynote.service.RecurringService;

/**
 * 분류 관리 창. 분류를 추가하고, 이름을 바꾸고, 쓰지 않는 분류를 지운다. (F5, UC5~UC7)
 *
 * <p>사용 건수 열로 삭제할 수 있는지 미리 보인다. 중복 이름과 사용 중 삭제는 서비스 계층이 막고,
 * 이 창은 그 메시지를 보여줄 뿐이다.
 */
public class CategoryDialog extends JDialog {

    private static final String[] COLUMNS = {"분류", "유형", "사용 건수", "기본 분류"};

    private final LedgerService ledger;
    private final RecurringService recurring;

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField nameField = new JTextField(12);
    private final JRadioButton expenseRadio = new JRadioButton("지출", true);
    private final JRadioButton incomeRadio = new JRadioButton("수입");

    /** 표의 행 순서와 같은 순서의 분류. 선택한 행을 분류로 바꾸는 데 쓴다. */
    private List<Category> rows = List.of();
    private boolean changed;

    /**
     * 분류 관리 창을 만든다. 모달이므로 {@code setVisible(true)} 는 창이 닫힐 때 돌아온다.
     *
     * @param owner     부모 창
     * @param ledger    분류 추가·변경·삭제를 맡는 서비스
     * @param recurring 분류를 쓰는 고정 지출 규칙을 세는 데 쓰는 서비스
     */
    public CategoryDialog(java.awt.Window owner, LedgerService ledger, RecurringService recurring) {
        super(owner, "분류 관리", ModalityType.APPLICATION_MODAL);
        this.ledger = ledger;
        this.recurring = recurring;

        buildLayout();
        reload();

        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * 분류가 하나라도 바뀌었는지. 호출한 화면이 분류 목록을 다시 채울지 판단하는 데 쓴다.
     *
     * @return 추가, 이름 변경, 삭제 중 하나라도 성공했으면 true
     */
    public boolean isChanged() {
        return changed;
    }

    private void buildLayout() {
        table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 고른 분류의 이름을 입력 칸에 옮겨 두면 이름 변경이 편하다.
        table.getSelectionModel().addListSelectionListener(e -> {
            Category selected = selectedCategory();
            if (!e.getValueIsAdjusting() && selected != null) {
                nameField.setText(selected.name());
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(420, 230));

        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(expenseRadio);
        typeGroup.add(incomeRadio);

        JButton add = new JButton("추가");
        JButton rename = new JButton("이름 변경");
        JButton delete = new JButton("삭제");
        add.addActionListener(e -> addCategory());
        rename.addActionListener(e -> renameSelected());
        delete.addActionListener(e -> deleteSelected());

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        form.add(new JLabel("이름"));
        form.add(nameField);
        form.add(expenseRadio);
        form.add(incomeRadio);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(add);
        buttons.add(rename);
        buttons.add(delete);
        JPanel center = new JPanel(new GridLayout(2, 1, 0, 6));
        center.add(form);
        center.add(buttons);

        JLabel hint = new JLabel("사용 중인 분류는 삭제할 수 없습니다. 먼저 거래의 분류를 바꾸세요.");
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

    /** 분류 목록과 사용 건수를 다시 읽어 표를 채운다. */
    private void reload() {
        rows = ledger.categories().findAll();
        model.setRowCount(0);
        for (Category c : rows) {
            model.addRow(new Object[] {
                c.name(), c.type().label(), ledger.usageCount(c.id()), c.seeded() ? "예" : ""
            });
        }
    }

    private void addCategory() {
        TransactionType type = expenseRadio.isSelected() ? TransactionType.EXPENSE
                : TransactionType.INCOME;
        try {
            ledger.addCategory(nameField.getText(), type);
        } catch (IllegalArgumentException e) {
            warn(e.getMessage());
            return;
        }
        afterChange();
    }

    private void renameSelected() {
        Category selected = requireSelection();
        if (selected == null) {
            return;
        }
        try {
            ledger.renameCategory(selected.id(), nameField.getText());
        } catch (IllegalArgumentException e) {
            warn(e.getMessage());
            return;
        }
        afterChange();
    }

    private void deleteSelected() {
        Category selected = requireSelection();
        if (selected == null) {
            return;
        }
        int rules = recurring.countByCategory(selected.id());
        if (rules > 0) {
            warn("이 분류를 사용하는 고정 지출 규칙이 " + rules + "개 있습니다. 먼저 규칙을 지우세요.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "'" + selected.name() + "' 분류를 삭제하시겠습니까?", "삭제 확인",
                JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            ledger.deleteCategory(selected.id());
        } catch (IllegalStateException e) {
            warn(e.getMessage());
            return;
        }
        afterChange();
    }

    private void afterChange() {
        changed = true;
        nameField.setText("");
        reload();
    }

    private Category selectedCategory() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rows.get(row);
    }

    private Category requireSelection() {
        Category selected = selectedCategory();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "먼저 목록에서 분류를 선택하세요.", "안내",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        return selected;
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 확인", JOptionPane.WARNING_MESSAGE);
    }
}
