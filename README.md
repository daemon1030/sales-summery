# Sales Summery

소규모 사업자가 수입·지출 항목과 일별 금액, 날짜별 특이사항을 관리하기 위한 Spring Boot 백엔드 프로젝트입니다.

> 현재 상태: **2~11단계 구현 완료**  
> 마지막 점검: 2026-08-02

## 브라우저 테스트 화면

애플리케이션 실행 후 `http://localhost:8080/`에 접속하면 간단한 API 테스트 화면을 사용할 수 있습니다.

- 회원가입과 로그인 후 JWT 자동 저장
- 내 정보와 수입·지출 항목 조회
- 항목, 수입·지출 기록, 날짜별 특이사항 등록
- 기간별 기록 조회와 대시보드 집계
- HTTP 상태 코드와 JSON 응답 확인

이 화면은 개발 중 API 동작 확인을 위한 도구이며, JWT는 현재 브라우저의 `localStorage`에만 저장됩니다.

## 현재 구현 범위

### 완료

- Spring Boot/Gradle 프로젝트 초기 설정
- PostgreSQL 운영 설정 및 H2 로컬·테스트 설정
- 생성·수정 시간을 한국 시간(`Asia/Seoul`)으로 기록하는 공통 엔티티
- 사용자, 수입·지출 항목, 금액 기록, 날짜별 특이사항 JPA 엔티티
- 각 엔티티의 Spring Data JPA Repository
- 주요 도메인 규칙과 DB 제약조건
- H2 PostgreSQL 호환 모드를 이용한 Repository 통합 테스트 작성
- 공통 성공·실패 응답과 오류 코드
- 전역 예외 처리 및 도메인별 Not Found 예외
- 요청 DTO 검증과 공통 날짜 범위 DTO
- 한국 시간 기준 Clock과 날짜·시간 직렬화 설정
- 회원가입, BCrypt 비밀번호 암호화 및 기본 항목 5개 자동 생성
- Spring Security 기반 JWT Access Token 인증
- 내 정보, 이름, 정산 시작일, 비밀번호 및 회원 탈퇴 API
- 사용자별 수입·지출 항목 조회·생성·이름 변경·활성 상태 관리
- 수입·지출 기록 CRUD와 기간·항목·거래 유형 검색 및 페이징
- 사용자별 날짜 특이사항 CRUD와 날짜·기간 조회
- 한국 시간 기준 정산 기간 계산 컴포넌트
- 오늘·정산 기간·선택 기간 손익, 항목별 합계 및 최근 기록 대시보드

### 이후 개선 후보

- Refresh Token과 로그아웃 토큰 무효화 정책
- 탈퇴 사용자 데이터의 보관·삭제 스케줄러
- CI 환경에서 PostgreSQL Testcontainers 통합 테스트 상시 실행
- 실제 트래픽을 기준으로 한 쿼리 실행 계획과 인덱스 추가 조정

## 구현된 도메인

| 도메인 | 구현 내용 |
| --- | --- |
| `User` | 로그인 ID 소문자 정규화, 형식 검증, 정산 시작일 1~28일 검증, 상태 및 탈퇴 시각 관리 |
| `FinancialCategory` | 수입·지출 구분, 고정·변동·해당 없음 비용 유형, 주기, 활성 상태 관리 |
| `FinancialRecord` | 항목별 날짜·금액·메모 저장, 0보다 큰 금액 검증 |
| `DailyNote` | 사용자별 날짜 특이사항 저장, 빈 내용 방지 |
| `BaseEntity` | 생성·수정 시각 자동 기록 |

현재 정의된 Enum은 다음과 같습니다.

- 사용자 상태: `ACTIVE`, `SUSPENDED`, `WITHDRAWN`
- 거래 유형: `INCOME`, `EXPENSE`
- 비용 유형: `FIXED`, `VARIABLE`, `NONE`
- 발생 주기: `DAILY`, `MONTHLY`, `IRREGULAR`

## 적용된 데이터 규칙

- 로그인 ID는 영문 소문자·숫자·밑줄로 구성된 4~10자이며, 입력값은 소문자로 정규화됩니다.
- 정산 시작일은 1~28일만 허용됩니다.
- 한 사용자 안에서는 항목명이 중복될 수 없습니다.
- 수입 항목의 비용 유형은 `NONE`만 허용됩니다.
- 지출 항목의 비용 유형은 `FIXED` 또는 `VARIABLE`만 허용됩니다.
- 금액은 `NUMERIC(15,2)`로 저장하며 0 이하를 허용하지 않습니다.
- 한 사용자에게 같은 날짜의 특이사항은 하나만 존재할 수 있습니다.
- 항목·기록 조회 Repository는 사용자 ID를 조건에 포함해 소유권 범위를 제한합니다.

## 프로젝트 구조

```text
가계부/
├── README.md
└── sales-summery/
    ├── build.gradle
    ├── gradlew / gradlew.bat
    └── src/
        ├── main/
        │   ├── java/com/example/sales_summery/
        │   │   ├── category/
        │   │   ├── dailynote/
        │   │   ├── financialrecord/
        │   │   ├── global/common/
        │   │   └── user/
        │   └── resources/
        └── test/
```

## 기술 스택

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA / Hibernate
- Spring Security / JWT
- Flyway
- PostgreSQL
- H2 (로컬 및 테스트, PostgreSQL 호환 모드)
- Bean Validation
- Lombok
- JUnit 5 / AssertJ
- Testcontainers
- Gradle Wrapper 9.5.1

## 실행 방법

### 사전 요구사항

- JDK 21
- PostgreSQL
- PostgreSQL 통합 테스트 실행 시 Docker

### PostgreSQL로 실행

기본 연결값은 `jdbc:postgresql://localhost:5432/sales_summery`, 사용자명은 `postgres`입니다. 필요하면 환경 변수로 변경할 수 있습니다.

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/sales_summery"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your-password"
$env:JWT_SECRET = "32바이트-이상의-충분히-긴-임의의-비밀값"
cd sales-summery
.\gradlew.bat bootRun
```

DB 스키마는 Flyway가 생성하며 JPA는 `ddl-auto: validate`로 엔티티와 스키마의 일치 여부만 검증합니다. 환경 변수 예시는 `sales-summery/.env.example`에서 확인할 수 있습니다.

### H2 로컬 프로필로 실행

```powershell
cd sales-summery
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

## 테스트 현황

작성된 테스트는 각 단계의 API, 서비스, Repository, 도메인 규칙을 다룹니다.

- 로그인 ID 및 정산 시작일 검증
- 정산 시작일 DB 기본값
- 사용자별 항목명 중복 제약
- 수입·비용 유형 조합 검증
- 동일 날짜·항목의 복수 금액 기록
- 사용자 소유 범위가 적용된 기록 조회
- 사용자·날짜별 특이사항 중복 제약
- 빈 특이사항과 0 이하 금액 거부

실행 명령:

```powershell
cd sales-summery
.\gradlew.bat test
```

프로젝트를 영문 경로로 이동해 기존 Gradle 테스트 클래스 로딩 문제를 해결했습니다.

2026-08-02 기준 전체 48개 테스트 중 47개가 통과하고 실패는 없습니다. PostgreSQL Testcontainers 테스트 1개는 현재 PC에 Docker가 없어 건너뛰었으며, Docker가 있는 개발·CI 환경에서는 실제 PostgreSQL과 Flyway 적용을 검증합니다.

## 단계별 구현 기록

### 2단계: 공통 API 기반 — 완료

- `ApiResponse<T>`와 `ErrorResponse`로 성공·실패 JSON 형식을 통일했습니다.
- `ErrorCode`에서 HTTP 상태, 오류 코드, 기본 메시지를 함께 관리합니다.
- `BusinessException`과 사용자·항목·기록·특이사항별 Not Found 예외를 추가했습니다.
- `GlobalExceptionHandler`가 비즈니스 오류, DTO 검증 실패, 타입 변환 실패, 잘못된 JSON 및 예상하지 못한 오류를 공통 응답으로 변환합니다.
- `DateRangeRequest`가 필수 날짜와 시작일·종료일 순서를 검증합니다.
- 한국 시간 `Clock`, `yyyy-MM-dd` 요청 날짜 형식 및 ISO 일시 직렬화 기반을 설정했습니다.
- 공통 응답, 예외 변환, 기간 검증, 한국 시간 설정 테스트를 추가했습니다.
- 검증 결과: `BUILD SUCCESSFUL`, 전체 15개 테스트 통과.

### 3단계: 회원가입 — 완료

- `POST /api/v1/auth/signup` 회원가입 API와 요청·응답 DTO를 추가했습니다.
- 로그인 아이디를 소문자로 정규화하고 사용자별 중복을 차단합니다.
- 비밀번호를 8~72자로 검증하고 BCrypt 해시만 DB에 저장합니다.
- 회원가입 시 카드 매출, 현금 매출, 재료비, 인건비, 월세 항목을 자동 생성합니다.
- 사용자 저장과 기본 항목 5개 생성을 하나의 `@Transactional` 작업으로 묶었습니다.
- 기본 항목 생성기를 강제로 실패시킨 통합 테스트에서 사용자 저장도 롤백되는 것을 확인했습니다.
- 사용자 이름 컬럼 길이를 명세와 동일한 50자로 수정했습니다.
- 검증 결과: `BUILD SUCCESSFUL`, 기존 테스트와 회원가입 테스트 전체 통과.

### 4단계: 로그인과 JWT 인증 — 완료

- `POST /api/v1/auth/login` 로그인 API와 요청·응답 DTO를 추가했습니다.
- 로그인 실패 시 아이디 존재 여부를 알 수 없는 `INVALID_CREDENTIALS` 응답을 사용합니다.
- HS256 JWT Access Token을 발급하며 만료 시간은 1시간입니다.
- JWT 비밀키는 소스에 저장하지 않고 `JWT_SECRET` 환경 변수로 주입합니다.
- `JwtAuthenticationFilter`가 Bearer Token을 검증하고 `AuthenticatedUser`를 SecurityContext에 저장합니다.
- 보호 API는 요청 DTO의 `userId`가 아닌 인증 principal의 사용자 ID를 사용합니다.
- 보호 요청마다 사용자 상태를 DB에서 확인해 정지·탈퇴 사용자의 기존 토큰도 차단합니다.
- 인증 실패와 권한 실패를 공통 API 오류 JSON으로 통일했습니다.
- 로그인 성공·실패, 1시간 만료, principal 전달, 무토큰·변조 토큰 및 탈퇴 사용자 차단을 통합 테스트했습니다.
- 검증 결과: `BUILD SUCCESSFUL`, 전체 테스트 통과.

### 5단계: 사용자 정보와 정산 설정 — 완료

- `GET /api/v1/users/me` 내 정보 조회 API를 추가했습니다.
- 이름 조회·변경과 정산 시작일 조회·변경 API를 추가했습니다.
- 정산 시작일은 요청 DTO와 도메인에서 모두 1~28일로 제한합니다.
- 비밀번호 변경 시 현재 비밀번호를 확인하고 새 비밀번호를 BCrypt로 다시 암호화합니다.
- 회원 탈퇴 시 현재 비밀번호를 확인하고 행 삭제 없이 상태와 탈퇴 시각만 변경합니다.
- 모든 API는 요청의 `userId`가 아닌 JWT 인증 principal의 사용자 ID를 사용합니다.
- 정보 변경, 잘못된 현재 비밀번호, 새 비밀번호 로그인, 탈퇴 상태 및 탈퇴 후 로그인 차단을 통합 테스트했습니다.
- 검증 결과: `BUILD SUCCESSFUL`, 전체 테스트 통과.

### 6단계: 수입·지출 항목 관리 — 완료

- 전체 항목과 활성 항목 목록, 상세 조회 API를 추가했습니다.
- 항목 추가, 이름 변경, 활성화 및 비활성화 API를 추가했습니다.
- 같은 사용자 안의 항목명 중복을 `DUPLICATE_CATEGORY_NAME`으로 차단합니다.
- 소유자 ID를 포함한 Repository 조회로 다른 사용자의 조회·변경을 `CATEGORY_NOT_FOUND`로 처리합니다.
- 생성 API만 거래 유형, 비용 유형, 발생 주기를 받고 이후에는 이름과 활성 상태만 변경할 수 있습니다.
- 수입은 `NONE`, 지출은 `FIXED` 또는 `VARIABLE`만 허용하는 도메인 규칙을 유지합니다.
- 생성·이름 변경·활성 상태·활성 목록·중복·소유권·유형 조합을 통합 테스트했습니다.
- 검증 결과: `BUILD SUCCESSFUL`, 전체 테스트 통과.

### 7단계: 수입·지출 기록 관리 — 완료

- 기록 등록, 상세 조회, 수정 및 삭제 API를 추가했습니다.
- 기록 수정 시 금액·날짜·메모와 함께 항목 변경을 지원합니다.
- 새 항목과 변경할 항목은 인증 사용자 소유의 활성 항목만 허용합니다.
- 시작일·종료일·항목·수입지출 구분을 조합한 검색을 구현했습니다.
- 최신 날짜순 기본 정렬과 허용된 정렬 필드, 페이지네이션을 지원하며 크기는 최대 100으로 제한합니다.
- Spring `Page` 내부 구조 대신 안정적인 `PageResponse<T>`를 반환합니다.
- 다른 사용자의 기록은 상세·수정·삭제 모두 `RECORD_NOT_FOUND`로 처리합니다.
- CRUD, 항목 이동, 필터·정렬·페이징, 비활성 항목, 소유권 및 잘못된 날짜 범위를 통합 테스트했습니다.
- 검증 결과: `BUILD SUCCESSFUL`, 전체 테스트 통과.

### 8단계: 날짜별 특이사항 — 완료

- 특이사항 등록, 특정 날짜 조회, 기간 조회, 내용 수정 및 삭제 API를 추가했습니다.
- 한 사용자에게 같은 날짜의 특이사항은 하나만 허용하고 중복 시 `DUPLICATE_DAILY_NOTE`를 반환합니다.
- 요청 DTO와 도메인에서 공백 내용 저장을 차단합니다.
- 기간 조회는 날짜 오름차순으로 반환하고 잘못된 날짜 범위를 차단합니다.
- 수정·삭제 Repository 조회에 인증 사용자 ID를 포함해 다른 사용자의 특이사항은 `DAILY_NOTE_NOT_FOUND`로 처리합니다.
- CRUD, 중복, 공백, 기간 정렬 및 소유권을 통합 테스트했습니다.
- 검증 결과: `BUILD SUCCESSFUL`, 전체 테스트 통과.

### 9단계: 정산 기간 계산 — 완료

- DB와 HTTP에 의존하지 않는 `SettlementPeriodCalculator`를 추가했습니다.
- 기준 날짜가 정산 시작일 이상이면 현재 달, 미만이면 이전 달에서 기간을 시작합니다.
- 종료일은 다음 정산 시작일의 전날로 계산하며 시작일과 종료일을 모두 포함합니다.
- 모든 현재 날짜 계산은 한국 시간 `Clock`을 사용합니다.
- 정산 시작일 1일·28일, 월·연도 경계, 2월과 윤년을 순수 단위 테스트했습니다.
- 검증 결과: `BUILD SUCCESSFUL`, 전체 테스트 통과.

### 10단계: 집계와 대시보드 — 완료

- 오늘, 현재 정산 기간, 사용자 지정 기간의 총수입·총지출·순이익 API를 추가했습니다.
- 기간별 항목 합계와 최근 수입·지출 기록 API를 추가했습니다.
- 수입·지출과 항목별 합계는 Repository 집계 쿼리에서 계산합니다.
- 순이익은 `totalIncome - totalExpense`로 계산합니다.
- 집계 데이터가 없으면 모든 금액을 `null`이 아닌 `0`으로 반환합니다.
- 최근 기록은 Pageable을 사용해 DB 조회 단계에서 5건으로 제한합니다.
- 빈 데이터, 손익 계산, 항목별 합계 및 최근 5건 제한을 통합 테스트했습니다.
- 검증 결과: `BUILD SUCCESSFUL`, 전체 테스트 통과.

### 11단계: 마무리 작업 — 완료

- 전체 API 경로와 JWT Bearer 인증 방식을 설명하는 `openapi.yaml`을 추가했으며 실행 중 `/openapi.yaml`로 조회할 수 있습니다.
- Flyway 초기 마이그레이션에 테이블, 제약조건과 사용자·활성 상태·기록 날짜 조회용 인덱스를 정의했습니다.
- JPA 자동 스키마 변경을 끄고 모든 프로필에서 `ddl-auto: validate`로 스키마를 검증합니다.
- 로컬 프로필에서 예시 사용자와 기본 항목을 만드는 반복 가능 테스트 데이터 마이그레이션을 추가했습니다.
- DB 접속값과 JWT 비밀값을 환경 변수로 분리하고 실제 `.env` 파일이 커밋되지 않도록 설정했습니다.
- 운영 로그 수준과 Flyway 설정을 담은 `application-prod.yml`을 추가했습니다.
- 실제 PostgreSQL 17 컨테이너에서 Flyway 적용 여부를 검사하는 통합 테스트를 추가했습니다.
- 검증 결과: `BUILD SUCCESSFUL`, 48개 중 47개 통과·1개 Docker 미설치로 건너뜀·실패 0개.

### 브라우저 API 테스트 화면 — 완료

- 별도 설치가 필요 없는 HTML·CSS·JavaScript 기반 테스트 화면을 추가했습니다.
- 로그인 성공 시 JWT를 저장하고 이후 보호 API 요청의 Bearer 헤더에 자동으로 포함합니다.
- 회원가입, 항목·기록·특이사항 등록, 기간 조회와 대시보드 집계를 화면에서 실행할 수 있습니다.
- HTTP 상태 코드와 공통 API JSON 응답을 화면의 응답 콘솔에서 확인할 수 있습니다.
- 테스트 화면과 정적 파일이 인증 없이 정상 제공되는지 통합 테스트로 검증했습니다.

## 참고

- 실제 애플리케이션 디렉터리 이름과 패키지 이름은 현재 `sales-summery`입니다. 영어 표기상 `summary`가 일반적이므로, 외부 공개나 배포 전에 이름을 유지할지 수정할지 결정하는 것이 좋습니다.
- 상세 요구사항과 구현 원칙은 `sales-summery/AGENTS.md`에 정리되어 있습니다.
