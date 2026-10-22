package moneynote.service;

/** 한 분류의 예산 대비 사용 현황. (F8) */
public class BudgetStatus {

    /** 예산 사용률 판정 단계. */
    public enum Level {
        /** 예산을 정하지 않았다. */
        UNSET("미설정"),
        /** 사용률 80% 미만. */
        NORMAL("정상"),
        /** 사용률 80% 이상 100% 미만. */
        WARNING("주의"),
        /** 사용률 100% 이상. */
        EXCEEDED("초과");

        private final String label;

        Level(String label) {
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
    }

    private final int categoryId;
    private final String categoryName;
    private final long used;
    private final long limit;
    private final Level level;

    /**
     * 현황을 만든다.
     *
     * @param categoryId   지출 분류 id
     * @param categoryName 분류 이름
     * @param used         사용액(원)
     * @param limit        한도(원), 없으면 0
     * @param level        판정 단계
     */
    public BudgetStatus(int categoryId, String categoryName, long used, long limit, Level level) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.used = used;
        this.limit = limit;
        this.level = level;
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
     * 분류 이름.
     *
     * @return 분류 이름
     */
    public String categoryName() {
        return categoryName;
    }

    /**
     * 사용액(원).
     *
     * @return 사용액(원)
     */
    public long used() {
        return used;
    }

    /**
     * 한도(원), 없으면 0.
     *
     * @return 한도(원), 없으면 0
     */
    public long limit() {
        return limit;
    }

    /**
     * 판정 단계.
     *
     * @return 판정 단계
     */
    public Level level() {
        return level;
    }

    /**
     * 사용률.
     *
     * @return 한도가 없으면 0, 있으면 사용률(%). 100을 넘을 수 있다.
     */
    public double usageRate() {
        return limit <= 0 ? 0.0 : used * 100.0 / limit;
    }

    /**
     * 한도를 넘은 금액.
     *
     * @return 초과 상태이면 사용액에서 한도를 뺀 금액, 아니면 0
     */
    public long overflow() {
        return Math.max(0L, used - limit);
    }
}
