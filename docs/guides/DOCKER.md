# Docker 실행 방법

프런트엔드는 Docker 이미지 빌드 중에 생성되어 Spring Boot 애플리케이션에 포함됩니다. 별도의 프런트엔드 컨테이너는 필요하지 않습니다.

## 1. 환경 변수 준비

프로젝트 루트에서 예시 파일을 복사합니다.

PowerShell:

```powershell
Copy-Item .env.example .env
```

Linux/macOS:

```bash
cp .env.example .env
```

`.env`의 `DB_PASSWORD`와 `JWT_SECRET`은 반드시 안전한 값으로 변경합니다. JWT 키는 32바이트 이상의 무작위 값을 사용합니다.

예시 생성 명령:

```bash
openssl rand -base64 48
```

## 2. 빌드 및 실행

```bash
docker compose up --build -d
```

PostgreSQL이 정상 상태가 된 뒤 애플리케이션이 시작되며 Flyway가 DB 스키마를 자동 적용합니다.

## 3. 접속

기본 포트는 8080입니다.

```text
http://localhost:8080/service/
```

포트를 변경하려면 `.env`의 `APP_PORT`를 수정합니다.

## 운영 명령

로그 확인:

```bash
docker compose logs -f app
```

상태 확인:

```bash
docker compose ps
```

중지:

```bash
docker compose down
```

이미지 재빌드 및 재시작:

```bash
docker compose up --build -d
```

`docker compose down`은 DB 볼륨을 삭제하지 않으므로 데이터가 보존됩니다. DB까지 완전히 삭제하는 `docker compose down -v`는 저장된 가계부 데이터를 제거하므로 초기화가 명확히 필요한 경우에만 사용합니다.
