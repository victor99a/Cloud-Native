# Credenciales de prueba — Pedidos360

> **Uso exclusivamente educativo (DSY1107, Duoc UC).** Son cuentas de demostración de un
> laboratorio temporal: no reutilizar estas claves en ningún sistema real.
> Las claves generadas al azar (PostgreSQL y RabbitMQ en AWS) **no** se versionan: viven solo en
> el `.env` de cada EC2 y se obtienen con los comandos de la sección 3.

## 1. Nube (AWS) — usuarios de la aplicación (Amazon Cognito)

URL de la aplicación (API Manager): la imprime `deploy.sh` al final.
Último despliegue: `https://136xrpjdoj.execute-api.us-east-1.amazonaws.com`.

| Usuario | Clave | Grupo (rol) | Puede |
|---------|-------|-------------|-------|
| `admin` | `Pedidos360Admin` | `ADMIN` | Todo: ver, crear, cambiar estado y eliminar pedidos; CRUD de productos |
| `operador` | `Pedidos360Oper` | `OPERADOR` | Igual que admin en pedidos y productos |
| `cliente` | `Pedidos360Clie` | `CLIENTE` | Ver pedidos, productos y analítica; **crear** pedidos. Cambiar estado o eliminar → **403** |

- User Pool: `pedidos360-tenant` · App client: `pedidos360-spa` · login por Hosted UI
  (Authorization Code + PKCE).
- Las claves son los valores por defecto de `infra/aws/deploy.sh` (`ADMIN_PASSWORD`,
  `OPERADOR_PASSWORD`, `CLIENTE_PASSWORD`); se pueden cambiar con esas variables al desplegar.

### Matriz de permisos (BFF)

| Endpoint | Sin token | `cliente` | `operador` / `admin` |
|----------|-----------|-----------|----------------------|
| `GET /api/pedidos`, `GET /api/productos`, `GET /api/analitica/*` | 401 | 200 | 200 |
| `POST /api/pedidos` | 401 | 201 | 201 |
| `PATCH /api/pedidos/{id}/estado`, `DELETE /api/pedidos/{id}` | 401 | **403** | 200 / 204 |
| `POST/PUT/DELETE /api/productos` | 401 | **403** | 2xx |

Otros casos útiles para la demo: token alterado → 401; **ID token** en lugar de access token → 401;
llamada directa a la EC2 (`http://<IP-apps>:8080/api/...`, sin pasar por API Gateway) → 403.

## 2. Nube (AWS) — consolas de administración

| Herramienta | URL | Acceso |
|-------------|-----|--------|
| Kafka UI | `http://<IP-messaging>:8091` (último: `44.213.34.232`) | Sin login; solo desde la IP que ejecutó `deploy.sh` |
| RabbitMQ (nodo 1 / nodo 2) | `http://<IP-messaging>:15672` / `:15673` | Usuario `pedidos360`; clave en el `.env` (sección 3); solo desde la IP del despliegue |
| SSH a las EC2 | `ssh -i ~/.ssh/labsuser.pem ec2-user@<IP>` | Llave `labsuser.pem` del Learner Lab |

Si cambias de red (otra IP), vuelve a ejecutar `SKIP_CODE=1 bash infra/aws/deploy.sh` para que el
security group autorice la IP nueva.

## 3. Nube (AWS) — claves generadas (no versionadas)

```bash
# RabbitMQ (usuario pedidos360) y PostgreSQL (usuario postgres, base pedidos360)
ssh -i ~/.ssh/labsuser.pem ec2-user@<IP-messaging> \
  'grep -E "^(RABBITMQ_USERNAME|RABBITMQ_PASSWORD|DB_USERNAME|DB_PASSWORD)=" ~/pedidos360/infra/aws/.env'

# Consola SQL directa
ssh -i ~/.ssh/labsuser.pem ec2-user@<IP-apps> \
  'docker exec -it aws-postgres-1 psql -U postgres -d pedidos360'
```

## 4. Entorno local (Docker Compose, `infra/apps/docker-compose.yml`)

| Servicio | URL | Usuario / clave |
|----------|-----|-----------------|
| App (frontend) | `http://localhost:4200` | ver usuarios de Keycloak abajo |
| Keycloak — usuarios del realm `pedidos360` | `http://localhost:8090` | `admin` / `admin123` (ADMIN) · `operador` / `operador123` (OPERADOR) · `cliente` / `cliente123` (CLIENTE) |
| Keycloak — consola de administración | `http://localhost:8090/admin` | `admin` / `admin` |
| RabbitMQ (nodo 1 / nodo 2) | `http://localhost:15672` / `:15673` | `guest` / `guest` |
| Kafka UI | `http://localhost:8091` | sin login |
| PostgreSQL | `localhost:5432`, base `pedidos360` | `postgres` / `postgres` |

## 5. Obtener un token por línea de comandos (pruebas con curl)

```bash
API=https://136xrpjdoj.execute-api.us-east-1.amazonaws.com
CLIENT_ID=3co912n6as17utm5q0a07upsrl   # lo imprime deploy.sh (Cognito client)

TOKEN=$(aws cognito-idp initiate-auth --auth-flow USER_PASSWORD_AUTH --client-id "$CLIENT_ID" \
  --auth-parameters USERNAME=cliente,PASSWORD=Pedidos360Clie \
  --query AuthenticationResult.AccessToken --output text)

curl -i "$API/api/pedidos"                                       # 401 sin token
curl -i -H "Authorization: Bearer $TOKEN" "$API/api/pedidos"     # 200
curl -i -X PATCH -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"estado":"ENVIADO"}' "$API/api/pedidos/2/estado"          # 403 (cliente)
```

En otra cuenta del lab, la URL, el client id y las IP cambian: `deploy.sh` los imprime al terminar.
