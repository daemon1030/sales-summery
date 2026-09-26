# Sales Summary

한 명의 사용자가 하나의 사업장을 관리한다는 전제로 수입·지출, 날짜별 특이사항, 정산 기간별 손익을 관리하는 웹 애플리케이션입니다. Spring Boot REST API와 Vue SPA로 구성되며, JWT 인증과 사용자별 데이터 소유권 검증을 적용합니다.

> 저장소의 디렉터리명과 Gradle 프로젝트명은 기존 이름인 `sales-summery`를 사용하지만, 문서에서는 서비스 의미에 맞춰 **Sales Summary**로 표기합니다.

## 주요 기능

- 회원가입, 로그인, 내 정보 및 비밀번호·정산 시작일 관리
- 회원가입 시 기본 수입·지출 항목 5개 자동 생성
- 수입·지출 항목의 등록, 조회, 이름 변경, 활성화·비활성화
- 수입·지출 기록의 등록, 검색, 수정, 삭제 및 페이지네이션
- 날짜별 특이사항의 등록, 기간 조회, 수정, 삭제
- 오늘·현재 정산 기간·사용자 지정 기간의 수입, 지출, 순이익 집계
- 일·월·연도별 손익 추이, 항목별 합계와 최근 기록 5건 조회
- 엑셀 양식 다운로드, 파일 분석, 열 매핑, 중복 방지 후 일괄 가져오기
- Vue 기반 대시보드와 관리 화면, 브라우저용 API 테스트 화면

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1, Spring Web MVC |
| Persistence | Spring Data JPA, PostgreSQL 16, H2(local/test) |
| Security | Spring Security, JWT Access Token, BCrypt |
| Schema | Flyway, PostgreSQL 호환 SQL |
| Excel | Apache POI |
| Frontend | Vue 3, TypeScript, Vite, Pinia, Vue Router, Chart.js |
| Build/Test | Gradle Wrapper, JUnit 5, Testcontainers |
| Deployment | Docker, Docker Compose |

## 아키텍처

기능별 패키지를 사용하는 모듈형 모놀리스입니다. 일반적인 API 요청은 다음 순서로 처리됩니다.

```text
Vue SPA / API Client
        │ HTTP + Bearer JWT
        ▼
Spring Security Filter
        ▼
Controller → Service → Repository → PostgreSQL
                  │
                  └→ Domain Entity / DTO
```

- Controller는 HTTP 요청·응답, DTO 검증, 인증 사용자 전달을 담당합니다.
- Service는 트랜잭션, 소유권, 활성 상태와 비즈니스 규칙을 검증합니다.
- Repository는 사용자 조건이 포함된 조회와 집계 쿼리를 수행합니다.
- Entity는 상태 변경 메서드를 제공하며 API에는 요청·응답 DTO만 노출합니다.
- `JwtAuthenticationFilter`는 보호 API 요청마다 토큰을 검증하고 사용자의 현재 상태를 DB에서 다시 확인합니다.
- 시간 계산은 `Asia/Seoul`, 금액은 `NUMERIC(15,2)`/`BigDecimal`을 기준으로 합니다.

## 프로젝트 구조

```text
sales-summery/
├─ frontend/                         # Vue 3 SPA 원본
│  ├─ src/
│  │  ├─ api/                        # HTTP 클라이언트와 API 타입
│  │  ├─ components/                 # 모달, 상태 패널, 차트
│  │  ├─ layouts/                    # 인증/서비스 공통 레이아웃
│  │  ├─ pages/                      # 로그인, 대시보드, 기록, 가져오기 등
│  │  ├─ stores/                     # Pinia 인증 상태
│  │  └─ router.ts                   # 화면 라우팅과 인증 가드
│  ├─ package.json
│  └─ vite.config.ts                 # /api 프록시, /service 빌드 설정
├─ docs/
│  ├─ guides/                        # 실행·운영 가이드
│  │  └─ DOCKER.md
│  └─ reviews/                       # 코드 리뷰와 구현 로드맵
│     ├─ CODE_REVIEW.html
│     └─ walkthrough-service-frontend-roadmap.html
├─ samples/
│  └─ excel/                         # 엑셀 가져오기 예시 원본
├─ outputs/
│  ├─ docker/                        # Docker 이미지 내보내기 산출물
│  └─ excel-import/                  # 생성된 엑셀 양식
├─ src/main/java/com/example/sales_summery/
│  ├─ auth/                          # 회원가입·로그인, JWT 보안
│  ├─ user/                          # 사용자 정보, 비밀번호, 정산 시작일
│  ├─ category/                      # 수입·지출 항목
│  ├─ financialrecord/               # 수입·지출 기록과 검색
│  ├─ financialrecordimport/         # 엑셀 분석·매핑·일괄 저장
│  ├─ dailynote/                     # 날짜별 특이사항
│  ├─ dashboard/                     # 손익 집계와 추이
│  ├─ settlement/                    # 정산 기간 계산
│  ├─ global/                        # 설정, 공통 응답, 예외 처리
│  └─ SalesSummeryApplication.java   # 애플리케이션 진입점
├─ src/main/resources/
│  ├─ db/migration/                  # 운영/공통 Flyway 마이그레이션
│  ├─ db/local/                      # local 프로필 샘플 데이터
│  ├─ static/service/                # 빌드된 Vue SPA
│  ├─ static/api-test/               # 브라우저 API 테스트 도구
│  ├─ static/openapi.yaml            # API 명세
│  └─ application*.yml               # 공통/local/prod 설정
├─ src/test/                         # 서비스·보안·DB 통합 테스트
├─ compose.yml                       # 애플리케이션 + PostgreSQL
├─ Dockerfile                        # 프런트/백엔드 멀티 스테이지 빌드
├─ build.gradle
└─ README.md
```

## 실행 전 준비

필요한 도구는 실행 방식에 따라 다릅니다.

- 백엔드 로컬 실행: JDK 21
- 프런트엔드 개발 서버: Node.js 22 권장, pnpm(Corepack)
- Docker 실행: Docker Engine 및 Docker Compose
- PostgreSQL 직접 실행: PostgreSQL 16 권장

Gradle은 Wrapper를 사용하므로 별도 설치하지 않아도 됩니다. Windows에서는 `gradlew.bat`, macOS/Linux에서는 `./gradlew`를 사용합니다.

## 가장 빠른 실행: local 프로필

기본 프로필이 `local`이므로 PostgreSQL 설치 없이 H2 인메모리 DB와 샘플 데이터로 실행할 수 있습니다.

```powershell
.\gradlew.bat bootRun
```

macOS/Linux:

```bash
./gradlew bootRun
```

실행 후 접속 주소:

- 서비스 화면: <http://localhost:8080/service/>
- API 테스트 화면: <http://localhost:8080/api-test/>
- OpenAPI 문서 원본: <http://localhost:8080/openapi.yaml>

`local` DB는 애플리케이션을 종료하면 사라집니다. Flyway가 스키마와 로컬 샘플 데이터를 시작 시 적용합니다.

## 프런트엔드 개발 서버 실행

백엔드를 `localhost:8080`에서 먼저 실행한 뒤 새 터미널에서 실행합니다. Vite가 `/api` 요청을 백엔드로 프록시합니다.

```powershell
cd frontend
corepack enable
pnpm install --frozen-lockfile
pnpm dev
```

개발 화면은 <http://localhost:5173/service/>에서 확인합니다.

프런트엔드 정적 파일을 Spring Boot 리소스로 다시 빌드하려면 다음 명령을 사용합니다.

```powershell
cd frontend
pnpm build
```

결과는 `src/main/resources/static/service/`에 생성됩니다.

## Docker Compose로 실행

Docker Compose는 Vue 빌드, Spring Boot 빌드, PostgreSQL 실행을 함께 처리합니다.

1. 환경 변수 예시 파일을 복사합니다.

   ```powershell
   Copy-Item .env.example .env
   ```

2. `.env`에서 `DB_PASSWORD`와 `JWT_SECRET`을 안전한 값으로 변경합니다. `JWT_SECRET`은 최소 32바이트의 무작위 값을 권장합니다.

3. 컨테이너를 빌드하고 실행합니다.

   ```powershell
   docker compose up --build -d
   ```

4. <http://localhost:8080/service/>에 접속합니다. `APP_PORT`를 변경했다면 해당 포트를 사용합니다.

상태와 로그 확인:

```powershell
docker compose ps
docker compose logs -f app
```

종료:

```powershell
docker compose down
```

`docker compose down`은 PostgreSQL 볼륨을 보존합니다. `down -v`는 저장 데이터까지 삭제하므로 초기화가 명확히 필요한 경우에만 사용하세요.

## PostgreSQL에 직접 연결해 실행

PostgreSQL에 `sales_summery` 데이터베이스를 만든 뒤 환경 변수를 설정합니다.

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:postgresql://localhost:5432/sales_summery"
$env:DB_USERNAME = "sales_app"
$env:DB_PASSWORD = "your-password"
$env:JWT_SECRET = "replace-with-a-random-secret-of-at-least-32-bytes"
.\gradlew.bat bootRun
```

Flyway가 `src/main/resources/db/migration`의 SQL을 적용하고 JPA는 결과 스키마를 검증합니다(`ddl-auto=validate`). 운영 비밀번호와 JWT 키는 소스에 저장하지 마세요.

## 빌드 및 테스트

전체 테스트:

```powershell
.\gradlew.bat test
```

애플리케이션 JAR 빌드:

```powershell
.\gradlew.bat clean bootJar
```

프런트엔드 타입 검사와 빌드:

```powershell
cd frontend
pnpm typecheck
pnpm build
```

일부 PostgreSQL 통합 테스트는 Docker를 사용하는 Testcontainers 기반이므로 Docker 데몬이 필요할 수 있습니다.

## API 개요

Base URL은 `/api/v1`이며 회원가입과 로그인을 제외한 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

| 경로 | 역할 |
| --- | --- |
| `/auth` | 회원가입, 로그인 |
| `/users/me` | 내 정보, 이름·비밀번호·정산 시작일 변경, 탈퇴 |
| `/categories` | 수입·지출 항목 관리 |
| `/financial-records` | 기록 등록, 검색, 수정, 삭제 |
| `/daily-notes` | 날짜별 특이사항 관리 |
| `/dashboard` | 기간 손익, 추이, 항목 비중, 최근 기록 |
| `/financial-record-imports` | 엑셀 양식, 분석, 매핑 확정 |

성공 응답은 `{"success": true, "data": ...}`, 실패 응답은 `{"success": false, "error": {"code": "...", "message": "..."}}` 형식을 사용합니다. 세부 요청·응답은 `src/main/resources/static/openapi.yaml`을 참고하세요.

## 데이터 모델 핵심

- `users` 1 — N `financial_categories`
- `financial_categories` 1 — N `financial_records`
- `users` 1 — N `daily_notes`
- `users` 1 — N `financial_record_imports`
- 사용자와 정규화된 엑셀 열 이름별로 `financial_import_column_mappings`를 저장해 다음 가져오기에서 자동 매핑합니다.

모든 사용자 데이터 조회는 인증 정보의 사용자 ID를 기준으로 제한합니다. 다른 사용자의 리소스는 존재하지 않는 것처럼 처리합니다. 수입은 `costType=NONE`, 지출은 `FIXED` 또는 `VARIABLE`만 허용하고, 금액은 항상 0보다 커야 합니다.

## 코드 리뷰 문서

[CODE_REVIEW.html](./docs/reviews/CODE_REVIEW.html)을 브라우저로 열면 패키지·주요 클래스 역할, 데이터 모델, JWT 요청 흐름, 회원가입, 기록 저장, 정산 집계, 엑셀 가져오기 동작을 Mermaid 다이어그램과 함께 볼 수 있습니다.

Docker 기반 실행과 운영 명령만 빠르게 확인하려면 [Docker 실행 가이드](./docs/guides/DOCKER.md)를 참고하세요.

Mermaid는 CDN 모듈을 사용하므로 다이어그램을 렌더링하려면 최초 열람 시 인터넷 연결이 필요합니다. 연결이 없어도 본문과 Mermaid 원문은 확인할 수 있습니다.
