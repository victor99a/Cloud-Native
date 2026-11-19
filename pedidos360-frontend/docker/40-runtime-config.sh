#!/bin/sh
# Genera /config.json para el frontend a partir de variables de entorno.
# Lo ejecuta automáticamente la imagen oficial de nginx al arrancar
# (/docker-entrypoint.d). Sin AUTH_ISSUER no se genera nada y la app usa los
# valores compilados (entorno local con Keycloak).
set -eu

TARGET=/usr/share/nginx/html/config.json

if [ -z "${AUTH_ISSUER:-}" ]; then
  rm -f "$TARGET"
  echo "[runtime-config] AUTH_ISSUER no definido: se usa la configuración compilada"
  exit 0
fi

cat > "$TARGET" <<EOF
{
  "apiGatewayUrl": "${API_BASE_URL:-}",
  "auth": {
    "issuer": "${AUTH_ISSUER}",
    "clientId": "${AUTH_CLIENT_ID}",
    "scope": "${AUTH_SCOPE:-openid profile email}",
    "requireHttps": ${AUTH_REQUIRE_HTTPS:-true},
    "logoutUrl": "${AUTH_LOGOUT_URL:-}"
  }
}
EOF
echo "[runtime-config] config.json generado (issuer=${AUTH_ISSUER})"
