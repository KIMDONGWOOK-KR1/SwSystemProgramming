package moneynote.service;

/** 한 분류가 차지하는 지출 금액과 비중. (F7) */
public class CategoryShare {

    private final int categoryId;
    private final String categoryName;
    private final long amount;
    private final double percent;

    /**
     * 비중 한 줄을 만든다.
     *
     * @param categoryId   분류 id, 기타로 묶은 줄이면 -1
     * @param categoryName 분류 이름
     * @param amount       지출 합계(원)
     * @param percent      그 달 전체 지출에서 차지하는 비중(%)
     */
    public CategoryShare(int categoryId, String categoryName, long amount, double percent) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.amount = amount;
        this.percent = percent;
    }

    /**
     * 분류 id, 기타로 묶은 줄이면 -1.
     *
     * @return 분류 id, 기타로 묶은 줄이면 -1
     */
    public int categoryId() {
        return categoryId;
    }

    /**
     * 분류 이름.
     *
     * @return 분류 이름
     */
    public String categoryName() {
        return categoryName;
    }

    /**
     * 지출 합계(원).
     *
     * @return 지출 합계(원)
     */
    public long amount() {
        return amount;
    }

    /**
     * 그 달 전체 지출에서 차지하는 비중(%).
     *
     * @return 그 달 전체 지출에서 차지하는 비중(%)
     */
    public double percent() {
        return percent;
    }
}
