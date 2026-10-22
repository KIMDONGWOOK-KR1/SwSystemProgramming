package moneynote.util;

import java.text.DecimalFormat;

/**
 * 금액 표시와 입력 해석을 한곳에서 담당한다.
 *
 * <p>화면 표시와 입력 파싱이 여기저기 흩어지면 형식이 어긋나므로 모두 이 클래스를 거친다.
 */
public final class MoneyFormat {

    private static final DecimalFormat COMMA = new DecimalFormat("#,##0");

    private MoneyFormat() {
    }

    /**
     * 단위를 붙여 표시한다. 1234500 -&gt; "1,234,500원"
     *
     * @param amount 금액(원)
     * @return 쉼표와 원이 붙은 문자열
     */
    public static String display(long amount) {
        return COMMA.format(amount) + "원";
    }

    /**
     * 단위 없이 표시한다. 1234500 -&gt; "1,234,500"
     *
     * @param amount 금액(원)
     * @return 쉼표만 붙은 문자열
     */
    public static String plain(long amount) {
        return COMMA.format(amount);
    }

    /**
     * 수입은 +, 지출은 - 를 붙여 표시한다.
     *
     * @param signedAmount 부호 있는 금액(원)
     * @return 예: "+1,679,000원", 0이면 +0원
     */
    public static String signed(long signedAmount) {
        String sign = signedAmount < 0 ? "-" : "+";
        return sign + COMMA.format(Math.abs(signedAmount)) + "원";
    }

    /**
     * 사용자가 입력한 문자열을 금액으로 해석한다. 콤마와 공백, "원"은 무시한다.
     *
     * @param input 사용자가 입력한 문자열
     * @return 금액(원)
     * @throws IllegalArgumentException 숫자로 볼 수 없거나 0 이하일 때
     */
    public static long parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("금액을 입력하세요.");
        }
        String cleaned = input.replace(",", "").replace("원", "").trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("금액을 입력하세요.");
        }
        long value;
        try {
            value = Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("금액은 숫자로만 입력하세요: " + input);
        }
        if (value <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
        return value;
    }
}
