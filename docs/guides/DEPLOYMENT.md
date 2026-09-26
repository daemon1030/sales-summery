# daemon1030.app 운영 배포

이 구성에서 인터넷에 공개되는 것은 Nginx의 80, 443 포트뿐입니다. Spring Boot(8080)와 PostgreSQL(5432)은 Docker 내부 네트워크에서만 통신합니다.

## 배포 전 준비

1. `daemon1030.app`의 DNS A 레코드를 서버 공인 IP로 연결합니다.
2. 서버 방화벽 또는 클라우드 보안 그룹에서 TCP 80, 443만 공개합니다. 8080과 5432는 공개하지 않습니다.
3. 서버에 Docker Engine 및 Docker Compose 플러그인을 설치합니다.
4. 프로젝트 루트에서 환경 파일을 준비합니다.

   ```bash
   cp .env.example .env
   ```

5. `.env`에서 `DB_PASSWORD`, `JWT_SECRET`, `LETSENCRYPT_EMAIL`을 실제 값으로 변경합니다. JWT 비밀값은 다음처럼 생성할 수 있습니다.

   ```bash
   openssl rand -base64 48
   ```

## 최초 HTTPS 인증서 발급 및 서비스 시작

DNS 반영이 끝난 뒤 Linux 서버에서 다음을 한 번 실행합니다.

```bash
chmod +x deploy/init-letsencrypt.sh
./deploy/init-letsencrypt.sh
```

스크립트는 임시 인증서로 Nginx를 먼저 시작하고, Let’s Encrypt에 정식 인증서를 요청한 뒤 Nginx를 다시 읽힙니다. 완료 후 서비스 주소는 다음입니다.

```text
https://daemon1030.app/service/
```

## 인증서 자동 갱신

Let’s Encrypt 인증서는 90일마다 갱신해야 합니다. 서버의 `crontab -e`에 다음을 추가해 매일 새벽 3시에 갱신 여부를 확인합니다. `/srv/sales-summery`는 실제 프로젝트 경로로 교체합니다.

```cron
0 3 * * * cd /srv/sales-summery && docker compose --profile maintenance run --rm certbot renew --webroot --webroot-path /var/www/certbot && docker compose exec -T nginx nginx -s reload
```

갱신이 필요하지 않은 날에는 인증서가 바뀌지 않습니다.

## 운영 확인

```bash
docker compose ps
docker compose logs -f nginx
docker compose logs -f app
```

HTTP 접속은 HTTPS로 이동해야 하며, 아래 두 주소는 외부에서 접속되면 안 됩니다.

```text
http://서버IP:8080
postgresql://서버IP:5432
```
