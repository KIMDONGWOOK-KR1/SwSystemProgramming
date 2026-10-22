package moneynote.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** 날짜 표시·해석과 월 경계 계산. */
public final class DateUtil {

    /** 날짜 형식 yyyy-MM-dd. */
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 월 형식 yyyy-MM. */
    public static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private DateUtil() {
    }

    /**
     * 날짜를 yyyy-MM-dd 로 쓴다.
     *
     * @param date 날짜
     * @return 예: 2026-10-22
     */
    public static String format(LocalDate date) {
        return date.format(DATE);
    }

    /**
     * yyyy-MM-dd 문자열을 날짜로 읽는다. 앞뒤 공백은 무시한다.
     *
     * @param input 사용자가 입력한 문자열
     * @return 날짜
     * @throws IllegalArgumentException yyyy-MM-dd 형식이 아닐 때
     */
    public static LocalDate parse(String input) {
        try {
            return LocalDate.parse(input.trim(), DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("날짜는 yyyy-MM-dd 형식으로 입력하세요: " + input);
        }
    }

    /**
     * 날짜가 그 달에 속하는지.
     *
     * @param date  날짜
     * @param month 달
     * @return 속하면 true
     */
    public static boolean isIn(LocalDate date, YearMonth month) {
        return YearMonth.from(date).equals(month);
    }

    /**
     * 지정한 달의 지정한 날짜를 구한다. 그 달에 없는 날이면 마지막 날로 대체한다.
     * 예: 2026-02 의 31일 -&gt; 2026-02-28 (F9의 예외 처리 규칙)
     *
     * @param month      달
     * @param dayOfMonth 날짜, 1~31
     * @return 그 달의 날짜
     */
    public static LocalDate dayInMonth(YearMonth month, int dayOfMonth) {
        int day = Math.min(dayOfMonth, month.lengthOfMonth());
        return month.atDay(Math.max(day, 1));
    }
}
