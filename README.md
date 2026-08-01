# Sales Summery

여러 사용자가 각자 하나의 사업장을 관리한다는 전제로, 수입·지출 기록과 날짜별 특이사항을 저장하고 기간별 손익을 조회하는 Spring Boot 백엔드 프로젝트입니다.

현재 구현 범위는 Spring Boot 기본 설정, PostgreSQL 연결 설정 예시, JPA 엔터티, Repository, Repository 통합 테스트까지입니다. 아직 Controller, Service, DTO, JWT 인증, 회원가입/로그인 API, 비즈니스 API, 대시보드 집계 API는 구현하지 않았습니다.

## 기술 스택

- Java 21
- Spring Boot 4.1.0
- Spring Data JPA
- Spring Web MVC
- Bean Validation
- PostgreSQL
- H2 Database
- Lombok
- Gradle

## 프로젝트 구조

패키지는 역할별로 한 번에 모으지 않고, 기능별로 먼저 나눈 뒤 그 안에서 역할을 나누는 방식입니다.

```text
src/main/java/com/example/sales_summery
├── SalesSummeryApplication.java
├── category
│   ├── domain
│   └── repository
├── dailynote
│   ├── domain
│   └── repository
├── financialrecord
│   ├── domain
│   └── repository
├── global
│   └── common
└── user
    ├── domain
    └── repository
```

이 구조는 `user`, `category`, `financialrecord`, `dailynote`처럼 기능 단위로 관련 코드를 모아두기 위한 구조입니다. 예를 들어 수입·지출 기록 기능을 수정할 때는 `financialrecord` 패키지를 중심으로 보면 됩니다.

## 설정 파일

### `build.gradle`

프로젝트의 Gradle 설정과 의존성을 관리합니다.

현재 포함된 주요 의존성은 Spring Web MVC, Spring Data JPA, Bean Validation, PostgreSQL 드라이버, H2, Lombok, 테스트 관련 의존성입니다.

### `src/main/resources/application.yml`

기본 실행 환경 설정 파일입니다.

- PostgreSQL 접속 정보를 환경변수로 받을 수 있게 설정했습니다.
- `ddl-auto: update`로 JPA가 스키마를 생성/수정할 수 있게 설정했습니다.
- Hibernate와 Jackson의 기준 시간대를 `Asia/Seoul`로 설정했습니다.

### `src/main/resources/application-local.yml`

로컬 실행용 H2 설정 파일입니다.

PostgreSQL 없이도 애플리케이션을 실행하거나 구조를 확인할 수 있도록 H2 메모리 DB를 PostgreSQL 호환 모드로 사용합니다.

### `src/test/resources/application-test.yml`

테스트 실행용 H2 설정 파일입니다.

테스트마다 스키마를 새로 만들고 제거하기 위해 `ddl-auto: create-drop`을 사용합니다.

## 공통 코드

### `SalesSummeryApplication`

Spring Boot 애플리케이션의 시작점입니다.

`main` 메서드에서 `SpringApplication.run()`을 호출해 서버를 실행합니다.

### `BaseEntity`

모든 엔터티가 공통으로 사용하는 생성일시와 수정일시를 관리합니다.

- `createdAt`: 최초 저장 시각
- `updatedAt`: 마지막 수정 시각
- `@PrePersist`: DB에 처음 저장되기 직전에 생성/수정 시각을 설정
- `@PreUpdate`: DB에 수정되기 직전에 수정 시각을 갱신

시간 기준은 한국 시간(`Asia/Seoul`)입니다.

## 사용자 도메인

### `User`

사용자 계정을 나타내는 JPA 엔터티입니다. 초기 버전에서는 사용자 1명이 하나의 사업장을 관리하는 주체로 취급됩니다.

주요 필드는 다음과 같습니다.

- `userId`: 사용자 PK
- `loginId`: 로그인 아이디
- `passwordHash`: 해시된 비밀번호
- `name`: 사용자 이름
- `status`: 사용자 상태
- `settlementStartDay`: 정산 시작일
- `withdrawnAt`: 탈퇴 시각

현재 적용된 주요 규칙은 다음과 같습니다.

- `login_id`는 DB에서 UNIQUE입니다.
- `loginId`는 4자 이상 10자 이하입니다.
- `loginId`는 영문자, 숫자, 밑줄만 허용합니다.
- `loginId`는 소문자로 정규화해 저장합니다.
- `settlementStartDay`는 1부터 28까지만 허용합니다.
- `settlement_start_day`의 DB 기본값은 1입니다.
- 탈퇴 시 실제 삭제 대신 상태를 `WITHDRAWN`으로 바꾸고 `withdrawnAt`을 기록합니다.

`User.create()`는 사용자 생성 시 검증을 거치도록 만든 정적 팩토리 메서드입니다. 생성자를 직접 열어두지 않고 생성 경로를 제한하기 위해 사용합니다.

### `UserStatus`

사용자 상태를 나타내는 Enum입니다.

- `ACTIVE`: 활성
- `SUSPENDED`: 정지
- `WITHDRAWN`: 탈퇴

DB에는 Enum 순서값이 아니라 문자열로 저장합니다.

### `UserRepository`

사용자 엔터티의 저장과 조회를 담당합니다.

현재 제공하는 주요 메서드는 다음과 같습니다.

- `findByLoginId`: 로그인 아이디로 사용자 조회
- `existsByLoginId`: 로그인 아이디 중복 확인

## 수입·지출 항목 도메인

### `FinancialCategory`

사용자별 수입·지출 항목을 나타내는 JPA 엔터티입니다.

예시는 카드 매출, 현금 매출, 재료비, 인건비, 월세 같은 항목입니다.

주요 필드는 다음과 같습니다.

- `categoryId`: 항목 PK
- `user`: 항목을 소유한 사용자
- `categoryName`: 항목명
- `transactionType`: 수입 또는 지출
- `costType`: 고정비, 변동비, 비용 없음
- `frequency`: 발생 주기
- `active`: 활성 여부

현재 적용된 주요 규칙은 다음과 같습니다.

- 같은 사용자 안에서는 `category_name`이 중복될 수 없습니다.
- 서로 다른 사용자는 같은 항목명을 사용할 수 있습니다.
- 수입 항목의 `costType`은 `NONE`만 허용합니다.
- 지출 항목의 `costType`은 `FIXED` 또는 `VARIABLE`만 허용합니다.
- 항목 삭제 대신 `deactivate()`로 비활성화할 수 있습니다.

`FinancialCategory`는 `User`와 다대일 관계입니다. 즉 여러 항목이 한 사용자에게 속할 수 있습니다.

### `TransactionType`

수입·지출 구분 Enum입니다.

- `INCOME`: 수입
- `EXPENSE`: 지출

### `CostType`

비용 유형 Enum입니다.

- `FIXED`: 고정 지출
- `VARIABLE`: 변동 지출
- `NONE`: 수입 항목에 사용하는 비용 없음 값

### `Frequency`

항목 발생 주기 Enum입니다.

- `DAILY`: 매일
- `MONTHLY`: 매월
- `IRREGULAR`: 비정기

### `FinancialCategoryRepository`

수입·지출 항목 저장과 조회를 담당합니다.

현재 제공하는 주요 메서드는 다음과 같습니다.

- `findByCategoryIdAndUserUserId`: 현재 사용자가 소유한 항목만 조회
- `existsByUserUserIdAndCategoryName`: 같은 사용자 안의 항목명 중복 확인
- `findAllByUserUserIdOrderByCategoryNameAsc`: 사용자 항목 전체 조회
- `findAllByUserUserIdAndActiveTrueOrderByCategoryNameAsc`: 사용자 활성 항목 조회

## 수입·지출 기록 도메인

### `FinancialRecord`

개별 수입·지출 기록을 나타내는 JPA 엔터티입니다.

주요 필드는 다음과 같습니다.

- `recordId`: 기록 PK
- `category`: 연결된 수입·지출 항목
- `recordDate`: 기록 날짜
- `amount`: 금액
- `memo`: 메모

현재 적용된 주요 규칙은 다음과 같습니다.

- 금액은 0보다 커야 합니다.
- 금액은 `NUMERIC(15,2)`에 맞게 `BigDecimal`로 저장합니다.
- 같은 날짜와 같은 항목의 여러 기록을 허용합니다.
- 기록 테이블에는 `user_id`를 중복 저장하지 않습니다.
- 수입·지출 여부는 연결된 `FinancialCategory`의 `transactionType`으로 판단합니다.
- `change()`를 통해 금액, 날짜, 메모, 항목을 수정할 수 있습니다.

`FinancialRecord`는 `FinancialCategory`와 다대일 관계입니다. 즉 여러 기록이 하나의 항목에 속할 수 있습니다.

### `FinancialRecordRepository`

수입·지출 기록 저장과 조회를 담당합니다.

현재 제공하는 주요 메서드는 다음과 같습니다.

- `findByRecordIdAndCategoryUserUserId`: 현재 사용자가 소유한 기록만 조회
- `findAllByCategoryUserUserIdAndRecordDateBetweenOrderByRecordDateDesc`: 사용자 기록을 날짜 범위와 최신순으로 조회

## 날짜별 특이사항 도메인

### `DailyNote`

사용자별 날짜 특이사항을 나타내는 JPA 엔터티입니다.

주요 필드는 다음과 같습니다.

- `noteId`: 특이사항 PK
- `user`: 특이사항을 소유한 사용자
- `noteDate`: 특이사항 날짜
- `content`: 내용

현재 적용된 주요 규칙은 다음과 같습니다.

- 한 사용자에게 같은 날짜의 특이사항은 하나만 존재할 수 있습니다.
- `content`는 공백만으로 저장할 수 없습니다.
- `changeContent()`를 통해 내용을 수정할 수 있습니다.

`DailyNote`는 `User`와 다대일 관계입니다. 즉 여러 날짜별 특이사항이 한 사용자에게 속할 수 있습니다.

### `DailyNoteRepository`

날짜별 특이사항 저장과 조회를 담당합니다.

현재 제공하는 주요 메서드는 다음과 같습니다.

- `findByUserUserIdAndNoteDate`: 특정 사용자의 특정 날짜 특이사항 조회
- `existsByUserUserIdAndNoteDate`: 같은 날짜 특이사항 중복 확인
- `findAllByUserUserIdAndNoteDateBetweenOrderByNoteDateAsc`: 기간 내 특이사항 조회

## 데이터베이스 관계

현재 엔터티 관계는 다음과 같습니다.

```text
users 1 ── N financial_categories
users 1 ── N daily_notes
financial_categories 1 ── N financial_records
```

외래키 이름은 관계를 알아보기 쉽게 명시했습니다.

- `fk_financial_categories_user`: 항목이 사용자를 참조
- `fk_financial_records_category`: 기록이 항목을 참조
- `fk_daily_notes_user`: 특이사항이 사용자를 참조

## DB 제약조건과 인덱스

현재 JPA 엔터티에 정의된 주요 DB 제약조건은 다음과 같습니다.

- `uk_users_login_id`: 로그인 아이디 중복 방지
- `chk_users_settlement_start_day`: 정산 시작일 1~28 제한
- `uk_financial_categories_user_name`: 같은 사용자 안의 항목명 중복 방지
- `chk_financial_categories_type_cost`: 수입·지출 유형과 비용 유형 조합 제한
- `chk_financial_records_amount`: 0 이하 금액 저장 방지
- `uk_daily_notes_user_date`: 같은 사용자 안의 같은 날짜 특이사항 중복 방지

현재 정의된 인덱스는 다음과 같습니다.

- `idx_financial_categories_user_active`: 사용자별 활성 항목 조회 최적화
- `idx_financial_records_category_date`: 항목과 날짜 기준 기록 조회 최적화

## 테스트

현재 `RepositoryIntegrationTest`에서 Repository와 DB 제약조건 중심의 통합 테스트를 검증합니다.

검증 중인 내용은 다음과 같습니다.

- 정산 시작일은 1~28만 허용
- 로그인 아이디는 4~10자, 영문자·숫자·밑줄만 허용
- 로그인 아이디는 소문자로 정규화
- `settlement_start_day` DB 기본값은 1
- 같은 사용자 안의 항목명 중복 차단
- 서로 다른 사용자의 같은 항목명 허용
- 수입 항목의 잘못된 비용 유형 차단
- 같은 날짜와 같은 항목의 여러 기록 허용
- 기록 조회 시 사용자 소유권 조건 적용
- 같은 사용자와 같은 날짜의 특이사항 중복 차단
- 공백 특이사항과 0 이하 금액 차단

테스트 실행 명령은 다음과 같습니다.

```bash
./gradlew test
```

## 실행 방법

PostgreSQL을 사용할 경우 환경변수로 DB 접속 정보를 설정한 뒤 실행합니다.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/sales_summery
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
./gradlew bootRun
```

PostgreSQL 없이 로컬 H2 설정으로 실행하려면 다음처럼 `local` 프로필을 사용합니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

현재는 Controller가 없기 때문에 브라우저에서 접속했을 때 보여줄 화면이나 API 응답은 아직 없습니다. 애플리케이션 실행과 JPA 스키마 생성 여부를 확인하는 단계입니다.

## 다음 구현 단계

AGENTS.md 기준 다음 권장 단계는 공통 응답 및 예외 처리입니다.

이후 순서는 다음과 같습니다.

1. 공통 성공/실패 응답 구조
2. 공통 예외 타입과 예외 처리기
3. 회원가입과 기본 항목 5개 자동 생성
4. 로그인과 JWT 인증
5. 사용자 정보 및 정산 시작일 API
6. 수입·지출 항목 API
7. 수입·지출 기록 API
8. 날짜별 특이사항 API
9. 대시보드 집계 API
