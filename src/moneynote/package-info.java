/**
 * MoneyNote 개인 가계부 프로그램의 진입점.
 *
 * <p>프로그램은 세 계층으로 나뉜다. 화면({@link moneynote.ui})은 응용 로직({@link moneynote.service})만
 * 부르고, 응용 로직은 저장소({@link moneynote.repository})를 통해 CSV 파일을 읽고 쓴다.
 * 계층 사이를 오가는 데이터는 {@link moneynote.model} 의 객체이다. {@link moneynote.Main} 이
 * 시작할 때 이 객체들을 한 번에 조립한다.
 */
package moneynote;
