package moneynote.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import moneynote.model.Category;
import moneynote.model.Transaction;
import moneynote.model.TransactionType;
import moneynote.service.LedgerService;
import moneynote.util.DateUtil;
import moneynote.util.MoneyFormat;

/**
 * 거래 등록·수정 폼. (F1, F2)
 *
 * <p>등록과 수정이 같은 검증 규칙을 따라야 하므로 화면 하나를 두 용도로 쓴다.
 * 검증 자체는 서비스 계층이 하고, 여기서는 형식 변환과 오류 안내만 담당한다.
 */
public class TransactionDialog extends JDialog {

    private static final String[] PAYMENTS = {"현금", "카드", "계좌이체"};

    private final LedgerService ledger;
    /** 수정 대상. 새로 등록하는 경우 null. */
    private final Transaction editing;

    private final JTextField dateField = new JTextField(12);
    private final JRadioButton expenseRadio = new JRadioButton("지출", true);
    private final JRadioButton incomeRadio = new JRadioButton("수입");
    private final JTextField amountField = new JTextField(12);
    private final JComboBox<Category> categoryBox = new JComboBox<>();
    private final JComboBox<String> paymentBox = new JComboBox<>(PAYMENTS);
    private final JTextField memoField = new JTextField(20);

    private boolean saved;

    /**
     * 거래 입력 창을 만든다. 모달이다.
     *
     * @param owner   부모 창
     * @param ledger  등록·수정을 맡는 서비스
     * @param editing 수정할 거래, 새로 등록하면 null
     */
    public TransactionDialog(java.awt.Window owner, LedgerService ledger, Transaction editing) {
        super(owner, editing == null ? "거래 등록" : "거래 수정", ModalityType.APPLICATION_MODAL);
        this.ledger = ledger;
        this.editing = editing;

        buildLayout();
        wireTypeSwitch();
        fillInitialValues();

        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * 저장 버튼으로 닫혔는지. 호출한 화면이 목록을 새로고칠지 판단하는 데 쓴다.
     *
     * @return 저장했으면 true, 취소했으면 false
     */
    public boolean isSaved() {
        return saved;
    }

    private void buildLayout() {
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(expenseRadio);
        typeGroup.add(incomeRadio);
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        typePanel.add(expenseRadio);
        typePanel.add(incomeRadio);

        form.add(new JLabel("날짜 (yyyy-MM-dd)"));
        form.add(dateField);
        form.add(new JLabel("유형"));
        form.add(typePanel);
        form.add(new JLabel("금액"));
        form.add(amountField);
        form.add(new JLabel("분류"));
        form.add(categoryBox);
        form.add(new JLabel("결제수단"));
        form.add(paymentBox);
        form.add(new JLabel("메모"));
        form.add(memoField);

        JButton save = new JButton("저장");
        JButton cancel = new JButton("취소");
        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
        buttons.add(cancel);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
    }

    /** 유형을 바꾸면 선택 가능한 분류 목록도 함께 바뀐다. */
    private void wireTypeSwitch() {
        expenseRadio.addActionListener(e -> reloadCategories());
        incomeRadio.addActionListener(e -> reloadCategories());
    }

    private void reloadCategories() {
        categoryBox.removeAllItems();
        for (Category c : ledger.categories().findByType(selectedType())) {
            categoryBox.addItem(c);
        }
    }

    private void fillInitialValues() {
        if (editing == null) {
            dateField.setText(DateUtil.format(LocalDate.now()));
            reloadCategories();
            return;
        }
        dateField.setText(DateUtil.format(editing.date()));
        expenseRadio.setSelected(editing.type() == TransactionType.EXPENSE);
        incomeRadio.setSelected(editing.type() == TransactionType.INCOME);
        amountField.setText(MoneyFormat.plain(editing.amount()));
        reloadCategories();
        ledger.categories().findById(editing.categoryId())
                .ifPresent(categoryBox::setSelectedItem);
        paymentBox.setSelectedItem(editing.payment());
        memoField.setText(editing.memo());
    }

    private TransactionType selectedType() {
        return incomeRadio.isSelected() ? TransactionType.INCOME : TransactionType.EXPENSE;
    }

    private void onSave() {
        try {
            LocalDate date = DateUtil.parse(dateField.getText());
            long amount = MoneyFormat.parse(amountField.getText());
            Category category = (Category) categoryBox.getSelectedItem();
            if (category == null) {
                throw new IllegalArgumentException("분류를 선택하세요.");
            }
            if (date.isAfter(LocalDate.now()) && !confirmFutureDate()) {
                return;
            }
            String payment = (String) paymentBox.getSelectedItem();
            String memo = memoField.getText();

            if (editing == null) {
                ledger.register(date, selectedType(), amount, category.id(), payment, memo);
            } else {
                ledger.update(editing.id(), date, selectedType(), amount, category.id(), payment, memo);
            }
            saved = true;
            dispose();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "입력 확인",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private boolean confirmFutureDate() {
        int answer = JOptionPane.showConfirmDialog(this,
                "미래 날짜입니다. 이대로 등록할까요?", "날짜 확인", JOptionPane.YES_NO_OPTION);
        return answer == JOptionPane.YES_OPTION;
    }
}
