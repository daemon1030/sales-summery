#!/usr/bin/env sh
set -eu

domain="${DOMAIN:?DOMAIN must be set in .env}"
email="${LETSENCRYPT_EMAIL:?LETSENCRYPT_EMAIL must be set in .env}"
cert_path="./certbot/conf/live/$domain/fullchain.pem"

mkdir -p ./certbot/www ./certbot/conf/live/"$domain"

# Nginx가 최초에도 기동할 수 있도록 임시 자체 서명 인증서를 만든다.
# 이후 Certbot이 이 파일을 정식 Let's Encrypt 인증서로 교체한다.
if [ ! -f "$cert_path" ]; then
  docker run --rm \
    -v "$PWD/certbot/conf:/etc/letsencrypt" \
    alpine/openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout "/etc/letsencrypt/live/$domain/privkey.pem" \
    -out "/etc/letsencrypt/live/$domain/fullchain.pem" \
    -subj "/CN=localhost"
fi

docker compose up -d --build

docker compose --profile maintenance run --rm certbot certonly \
  --webroot --webroot-path /var/www/certbot \
  --email "$email" --agree-tos --no-eff-email \
  -d "$domain" \
  --force-renewal

docker compose exec nginx nginx -s reload
