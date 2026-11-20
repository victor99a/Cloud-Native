# Usuarios de prueba — Pedidos360

> **Uso exclusivamente educativo (DSY1107, Duoc UC).** Cuentas de demostración de un laboratorio
> temporal: no reutilizar estas claves en ningún sistema real.

## Nube (AWS) — Amazon Cognito

URL de la aplicación: la imprime `deploy.sh` al final
(último despliegue: `https://136xrpjdoj.execute-api.us-east-1.amazonaws.com`).

| Usuario | Clave | Rol | Puede |
|---------|-------|-----|-------|
| `admin` | `Pedidos360Admin` | `ADMIN` | Ver, crear, cambiar estado y eliminar pedidos; CRUD de productos |
| `operador` | `Pedidos360Oper` | `OPERADOR` | Igual que admin en pedidos y productos |
| `cliente` | `Pedidos360Clie` | `CLIENTE` | Ver pedidos, productos y analítica; crear pedidos. Cambiar estado o eliminar → **403** |

Las claves son los valores por defecto de `infra/aws/deploy.sh` (`ADMIN_PASSWORD`,
`OPERADOR_PASSWORD`, `CLIENTE_PASSWORD`).

## Local (Docker Compose) — Keycloak

App en `http://localhost:4200`.

| Usuario | Clave | Rol |
|---------|-------|-----|
| `admin` | `admin123` | `ADMIN` |
| `operador` | `operador123` | `OPERADOR` |
| `cliente` | `cliente123` | `CLIENTE` |

## Permisos por endpoint

| Endpoint | Sin token | `cliente` | `operador` / `admin` |
|----------|-----------|-----------|----------------------|
| `GET /api/pedidos`, `GET /api/productos`, `GET /api/analitica/*` | 401 | 200 | 200 |
| `POST /api/pedidos` | 401 | 201 | 201 |
| `PATCH /api/pedidos/{id}/estado`, `DELETE /api/pedidos/{id}` | 401 | **403** | 200 / 204 |
| `POST/PUT/DELETE /api/productos` | 401 | **403** | 2xx |

## Token por línea de comandos (nube)

```bash
API=https://136xrpjdoj.execute-api.us-east-1.amazonaws.com
CLIENT_ID=3co912n6as17utm5q0a07upsrl   # lo imprime deploy.sh

TOKEN=$(aws cognito-idp initiate-auth --auth-flow USER_PASSWORD_AUTH --client-id "$CLIENT_ID" \
  --auth-parameters USERNAME=cliente,PASSWORD=Pedidos360Clie \
  --query AuthenticationResult.AccessToken --output text)

curl -i -H "Authorization: Bearer $TOKEN" "$API/api/pedidos"   # 200
```

En otra cuenta del lab la URL y el client id cambian: `deploy.sh` los imprime al terminar.
