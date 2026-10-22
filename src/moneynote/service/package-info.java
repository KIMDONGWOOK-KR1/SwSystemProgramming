/**
 * 응용 로직. 입력 검증, 조회, 집계, 예산 판정, 고정 지출 생성을 맡는다.
 *
 * <p>화면은 저장소를 직접 부르지 않고 이 패키지의 서비스를 거친다. 검증에 실패하면 서비스가
 * {@link java.lang.IllegalArgumentException} 이나 {@link java.lang.IllegalStateException} 을
 * 던지고, 화면은 그 메시지를 그대로 보여준다.
 */
package moneynote.service;
