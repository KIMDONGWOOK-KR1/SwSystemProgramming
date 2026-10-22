package moneynote.ui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import moneynote.model.Transaction;
import moneynote.repository.CategoryRepository;
import moneynote.util.DateUtil;
import moneynote.util.MoneyFormat;

/** 거래 목록을 JTable에 붙이기 위한 모델. (F3) */
public class TransactionTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"날짜", "유형", "분류", "금액", "결제수단", "메모"};

    private final CategoryRepository categories;
    private List<Transaction> rows = new ArrayList<>();

    /**
     * 빈 표 모델을 만든다.
     *
     * @param categories 분류 id를 이름으로 바꾸는 데 쓰는 저장소
     */
    public TransactionTableModel(CategoryRepository categories) {
        this.categories = categories;
    }

    /**
     * 표에 보일 거래를 바꾼다.
     *
     * @param rows 보일 거래, 이 순서대로 보인다
     */
    public void setRows(List<Transaction> rows) {
        this.rows = new ArrayList<>(rows);
        fireTableDataChanged();
    }

    /**
     * 행 번호의 거래. 정렬된 표라면 먼저 모델 행 번호로 바꿔서 부른다.
     *
     * @param rowIndex 모델 행 번호
     * @return 그 행의 거래
     */
    public Transaction at(int rowIndex) {
        return rows.get(rowIndex);
    }

    /**
     * 표에 보이는 모든 거래 (읽기 전용).
     *
     * @return 표에 보이는 모든 거래 (읽기 전용)
     */
    public List<Transaction> rows() {
        return List.copyOf(rows);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Transaction t = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> DateUtil.format(t.date());
            case 1 -> t.type().label();
            case 2 -> categories.nameOf(t.categoryId());
            case 3 -> MoneyFormat.plain(t.amount());
            case 4 -> t.payment();
            case 5 -> t.memo();
            default -> "";
        };
    }
}
