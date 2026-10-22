/**
 * 데이터 저장소. 객체를 메모리에 보관하고 UTF-8 CSV 파일로 읽고 쓴다.
 *
 * <p>저장소마다 파일 하나를 맡는다 (categories.csv, transactions.csv, budgets.csv, recurring.csv).
 * 형식이 깨진 행은 건너뛰고 행 번호를 기록해 두어 시작할 때 알릴 수 있게 한다.
 */
package moneynote.repository;
