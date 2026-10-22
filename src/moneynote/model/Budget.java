package moneynote.model;

import java.time.YearMonth;

/** 특정 분류의 한 달 예산 한도. (F8) */
public class Budget {

    private final int categoryId;
    private final YearMonth month;
    private long limitAmount;

    /**
     * 예산을 만든다.
     *
     * @param categoryId  지출 분류 id
     * @param month       적용할 달
     * @param limitAmount 한도(원)
     */
    public Budget(int categoryId, YearMonth month, long limitAmount) {
        this.categoryId = categoryId;
        this.month = month;
        this.limitAmount = limitAmount;
    }

    /**
     * 지출 분류 id.
     *
     * @return 지출 분류 id
     */
    public int categoryId() {
        return categoryId;
    }

    /**
     * 적용할 달.
     *
     * @return 적용할 달
     */
    public YearMonth month() {
        return month;
    }

    /**
     * 한도(원).
     *
     * @return 한도(원)
     */
    public long limitAmount() {
        return limitAmount;
    }

    /**
     * 한도를 바꾼다.
     *
     * @param limitAmount 새 한도(원)
     */
    public void setLimitAmount(long limitAmount) {
        this.limitAmount = limitAmount;
    }
}
