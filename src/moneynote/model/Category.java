package moneynote.model;

/** 수입·지출을 나누는 분류. 사용자가 직접 추가·수정할 수 있다. (F5) */
public class Category {

    private final int id;
    private String name;
    private final TransactionType type;
    /** 최초 실행 시 자동 생성된 기본 분류인지 여부. */
    private final boolean seeded;

    /**
     * 분류를 만든다.
     *
     * @param id     저장소가 붙인 고유 번호
     * @param name   이름
     * @param type   이 분류를 쓸 수 있는 거래 유형
     * @param seeded 최초 실행 때 만든 기본 분류이면 true
     */
    public Category(int id, String name, TransactionType type, boolean seeded) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.seeded = seeded;
    }

    /**
     * 분류 id.
     *
     * @return 분류 id
     */
    public int id() {
        return id;
    }

    /**
     * 분류 이름.
     *
     * @return 분류 이름
     */
    public String name() {
        return name;
    }

    /**
     * 이름을 바꾼다. 중복 검사는 {@code LedgerService.renameCategory} 가 먼저 한다.
     *
     * @param name 새 이름
     */
    public void rename(String name) {
        this.name = name;
    }

    /**
     * 이 분류를 쓸 수 있는 거래 유형.
     *
     * @return 이 분류를 쓸 수 있는 거래 유형
     */
    public TransactionType type() {
        return type;
    }

    /**
     * 최초 실행 때 만든 기본 분류이면 true.
     *
     * @return 최초 실행 때 만든 기본 분류이면 true
     */
    public boolean seeded() {
        return seeded;
    }

    @Override
    public String toString() {
        return name;
    }
}
