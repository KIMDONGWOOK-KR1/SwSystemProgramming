package moneynote.service;

import java.time.LocalDate;
import moneynote.model.TransactionType;

/**
 * 거래 조회 조건. (F4)
 *
 * <p>비어 있는(null) 조건은 무시하므로 일부만 지정해도 동작한다. 지정된 조건들은 AND로 결합된다.
 */
public class TransactionFilter {

    private LocalDate from;
    private LocalDate to;
    private TransactionType type;
    private Integer categoryId;
    private String keyword;

    private TransactionFilter() {
    }

    /**
     * 조건이 하나도 없는 필터. 이어서 조건 메서드를 연결해 쓴다.
     *
     * @return 모든 거래를 통과시키는 새 필터
     */
    public static TransactionFilter all() {
        return new TransactionFilter();
    }

    /**
     * 기간 조건. 시작일이 종료일보다 늦으면 사용자의 실수로 보고 두 값을 맞바꾼다.
     *
     * @param from 시작일(포함), null 이면 제한 없음
     * @param to   종료일(포함), null 이면 제한 없음
     * @return 이 필터
     */
    public TransactionFilter period(LocalDate from, LocalDate to) {
        // 시작일이 종료일보다 늦으면 사용자의 실수로 보고 두 값을 맞바꾼다.
        if (from != null && to != null && from.isAfter(to)) {
            this.from = to;
            this.to = from;
        } else {
            this.from = from;
            this.to = to;
        }
        return this;
    }

    /**
     * 유형 조건.
     *
     * @param type 수입 또는 지출, null 이면 둘 다
     * @return 이 필터
     */
    public TransactionFilter type(TransactionType type) {
        this.type = type;
        return this;
    }

    /**
     * 분류 조건.
     *
     * @param categoryId 분류 id, null 이면 모든 분류
     * @return 이 필터
     */
    public TransactionFilter category(Integer categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    /**
     * 메모 키워드 조건. 대소문자를 구분하지 않는다.
     *
     * @param keyword 메모에 들어 있어야 하는 글자, 비어 있으면 조건 없음
     * @return 이 필터
     */
    public TransactionFilter keyword(String keyword) {
        this.keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return this;
    }

    /**
     * 시작일, 없으면 null.
     *
     * @return 시작일, 없으면 null
     */
    public LocalDate from() {
        return from;
    }

    /**
     * 종료일, 없으면 null.
     *
     * @return 종료일, 없으면 null
     */
    public LocalDate to() {
        return to;
    }

    /**
     * 유형 조건, 없으면 null.
     *
     * @return 유형 조건, 없으면 null
     */
    public TransactionType type() {
        return type;
    }

    /**
     * 분류 조건, 없으면 null.
     *
     * @return 분류 조건, 없으면 null
     */
    public Integer categoryId() {
        return categoryId;
    }

    /**
     * 앞뒤 공백을 뗀 키워드, 없으면 null.
     *
     * @return 앞뒤 공백을 뗀 키워드, 없으면 null
     */
    public String keyword() {
        return keyword;
    }

    /**
     * 기간이 뒤바뀐 채 입력됐는지. 화면에서 사용자에게 알리는 데 쓴다.
     *
     * @param originalFrom 사용자가 시작일로 입력한 값
     * @return 시작일과 종료일을 맞바꿨으면 true
     */
    public boolean swapped(LocalDate originalFrom) {
        return originalFrom != null && from != null && !originalFrom.equals(from);
    }
}
