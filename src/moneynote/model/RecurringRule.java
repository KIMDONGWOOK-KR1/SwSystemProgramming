package moneynote.model;

import java.time.YearMonth;

/** 매월 반복되는 고정 지출 규칙. 월세, 구독료 등. (F9) */
public class RecurringRule {

    private final int id;
    private int categoryId;
    private long amount;
    /** 매월 결제일. 1~31. 해당 월에 없는 날이면 그 달의 마지막 날로 대체한다. */
    private int dayOfMonth;
    private String memo;
    private boolean active;
    /**
     * 이 규칙으로 마지막으로 거래를 만든 달. 아직 만든 적이 없으면 null.
     * 사용자가 자동 거래를 지워도 이 값은 남으므로 같은 달에 다시 만들지 않는다.
     */
    private YearMonth lastGenerated;

    /**
     * 규칙을 만든다.
     *
     * @param id            저장소가 붙인 고유 번호
     * @param categoryId    지출 분류 id
     * @param amount        매월 금액(원)
     * @param dayOfMonth    매월 결제일, 1~31
     * @param memo          생성될 거래의 메모, null 이면 빈 문자열로 둔다
     * @param active        켜져 있으면 true
     * @param lastGenerated 마지막으로 거래를 만든 달, 없으면 null
     */
    public RecurringRule(int id, int categoryId, long amount, int dayOfMonth,
                         String memo, boolean active, YearMonth lastGenerated) {
        this.id = id;
        this.categoryId = categoryId;
        this.amount = amount;
        this.dayOfMonth = dayOfMonth;
        this.memo = memo == null ? "" : memo;
        this.active = active;
        this.lastGenerated = lastGenerated;
    }

    /**
     * 규칙 id.
     *
     * @return 규칙 id
     */
    public int id() {
        return id;
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
     * 매월 금액(원).
     *
     * @return 매월 금액(원)
     */
    public long amount() {
        return amount;
    }

    /**
     * 매월 결제일, 1~31.
     *
     * @return 매월 결제일, 1~31
     */
    public int dayOfMonth() {
        return dayOfMonth;
    }

    /**
     * 생성될 거래의 메모.
     *
     * @return 생성될 거래의 메모
     */
    public String memo() {
        return memo;
    }

    /**
     * 켜져 있으면 true.
     *
     * @return 켜져 있으면 true
     */
    public boolean active() {
        return active;
    }

    /**
     * 규칙을 켜거나 끈다.
     *
     * @param active 켜려면 true, 끄려면 false
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * 마지막으로 거래를 만든 달, 아직 없으면 null.
     *
     * @return 마지막으로 거래를 만든 달, 아직 없으면 null
     */
    public YearMonth lastGenerated() {
        return lastGenerated;
    }

    /**
     * 마지막으로 거래를 만든 달을 기록한다.
     *
     * @param lastGenerated 거래를 만든 달
     */
    public void setLastGenerated(YearMonth lastGenerated) {
        this.lastGenerated = lastGenerated;
    }
}
