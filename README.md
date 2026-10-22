# MoneyNote

개인 가계부 데스크톱 프로그램 (Java Swing). 전남대학교 소프트웨어 시스템 설계(SSD/IP) 학기 프로젝트.

수입과 지출을 기록하고, 기간·분류별로 조회·집계하고, 분류별 월 예산을 넘으면 경고하며,
매달 나가는 고정 지출을 시작할 때 자동으로 거래로 만든다. 데이터는 `data/` 폴더의 UTF-8 CSV 파일에 저장한다.

## 기능

| 화면 | 기능 |
|---|---|
| 거래내역 탭 | 거래 등록·수정·삭제, 기간·유형·분류·메모 키워드 조회, 건수·수입·지출·잔액 요약 |
| 통계 탭 | 월 수지 요약, 전월 대비 지출 증감, 분류별 지출 비중 표와 막대그래프 |
| 예산 탭 | 분류별 월 한도 설정, 사용률에 따른 정상·주의·초과 판정 |
| 분류 관리 | 분류 추가, 이름 변경, 쓰지 않는 분류 삭제 |
| 고정 지출 | 매월 규칙 추가·끄기·삭제, 시작할 때 이번 달 거래를 한 번만 생성 |

## 실행

JDK 17 이상이 필요하다. 빌드 도구 없이 JDK 만 쓴다.

```powershell
.\run.ps1          # 컴파일 후 실행
.\run.ps1 -Test    # 서비스 계층 검증 하네스(SmokeTest) 실행
```

배포용 jar 는 `java -jar ssd_ip_cp4_2026f_220057_app.jar` 로 실행한다. 실행한 폴더 아래에 `data/` 가 만들어진다.
다른 폴더의 데이터를 쓰려면 경로를 인자로 준다: `java -jar ... D:\my-data`

## 구조

```
src/moneynote/
  Main.java      진입점. 저장소와 서비스를 조립하고 메인 창을 띄운다
  model/         거래, 분류, 예산, 고정 지출 규칙
  repository/    CSV 저장소 (categories, transactions, budgets, recurring)
  service/       검증, 조회, 집계, 예산 판정, 고정 지출 생성
  ui/            Swing 화면
  util/          날짜·금액 변환
test/            SmokeTest (JUnit 없이 순수 Java)
tools/           보고서 그림·제출물 생성 스크립트
```

화면은 서비스만 부르고 서비스가 저장소를 부른다. 검증 규칙은 서비스에 모여 있어서
사용자가 직접 등록할 때와 고정 지출 자동 생성이 같은 규칙을 따른다.

## 제출물 만들기

```powershell
.\tools\build_release.ps1 -StudentId 220057
```

`dist/` 에 실행 jar, Javadoc API 매뉴얼 zip, 소스 zip, 최종 제출 zip 이 만들어진다.
