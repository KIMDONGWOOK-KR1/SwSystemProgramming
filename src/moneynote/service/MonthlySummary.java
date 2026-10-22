package moneynote.service;

import java.time.YearMonth;

/** 한 달의 수지 요약과 전월 대비 증감. (F6) */
public class MonthlySummary {

    private final YearMonth month;
    private final long income;
    private final long expense;
    private final long previousExpense;
    private final boolean hasPrevious;

    /**
     * 요약을 만든다.
     *
     * @param month           요약한 달
     * @param income          총수입(원)
     * @param expense         총지출(원)
     * @param previousExpense 전월 총지출(원)
     * @param hasPrevious     전월에 거래가 하나라도 있으면 true
     */
    public MonthlySummary(YearMonth month, long income, long expense,
                          long previousExpense, boolean hasPrevious) {
        this.month = month;
        this.income = income;
        this.expense = expense;
        this.previousExpense = previousExpense;
        this.hasPrevious = hasPrevious;
    }

    /**
     * 요약한 달.
     *
     * @return 요약한 달
     */
    public YearMonth month() {
        return month;
    }

    /**
     * 총수입(원).
     *
     * @return 총수입(원)
     */
    public long income() {
        return income;
    }

    /**
     * 총지출(원).
     *
     * @return 총지출(원)
     */
    public long expense() {
        return expense;
    }

    /**
     * 잔액. 음수면 그 달에 번 것보다 많이 썼다는 뜻이다.
     *
     * @return 총수입 - 총지출(원)
     */
    public long balance() {
        return income - expense;
    }

    /**
     * 전월 대비 지출 증감액(원), 늘었으면 양수.
     *
     * @return 전월 대비 지출 증감액(원), 늘었으면 양수
     */
    public long expenseDelta() {
        return expense - previousExpense;
    }

    /**
     * 증감률을 계산할 수 있는지.
     * 전월 데이터가 없거나 전월 지출이 0이면 계산할 수 없다(0으로 나누기 방지).
     *
     * @return 계산할 수 있으면 true
     */
    public boolean hasExpenseRate() {
        return hasPrevious && previousExpense > 0;
    }

    /**
     * 전월 대비 지출 증감률. {@link #hasExpenseRate()} 가 false 이면 0이다.
     *
     * @return 증감률(%), 늘었으면 양수
     */
    public double expenseRate() {
        if (!hasExpenseRate()) {
            return 0.0;
        }
        return (expense - previousExpense) * 100.0 / previousExpense;
    }
}
