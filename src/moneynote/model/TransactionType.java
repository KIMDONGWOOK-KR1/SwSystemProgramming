package moneynote.model;

/** 거래 유형. 수입과 지출 두 가지뿐이다. */
public enum TransactionType {
    /** 수입. */
    INCOME("수입"),
    /** 지출. */
    EXPENSE("지출");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    /**
     * 화면에 보이는 한글 이름.
     *
     * @return 화면에 보이는 한글 이름
     */
    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
