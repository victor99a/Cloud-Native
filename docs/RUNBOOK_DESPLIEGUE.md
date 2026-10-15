# Runbook — Despliegue local de Pedidos360 (Docker Compose)

> Guía paso a paso para levantar todo el sistema en **local**, sin nube.
> Stack: Angular 18 + API Gateway (Spring Cloud Gateway) + 5 microservicios Spring Boot
> 3.3 (Java 21) + RabbitMQ (clúster 2 nodos) + Keycloak + PostgreSQL.

---

## 0. Prerrequisitos

- **Docker** (con Docker Compose v2): `docker --version` y `docker compose version`.
- **Git** (opcional, para clonar).
- Nada más: los servicios se compilan **dentro de Docker** (build multi-stage con Maven
  y Node), no hace falta tener Java/Maven/Node instalados en el host.

> Para compilar localmente sin Docker, ver el anexo de `docs/GUION_DEMO_EP4.md`.

---

## 1. Obtener el código

```bash
git clone https://github.com/victor99a/Cloud-Native.git
cd Cloud-Native
git checkout develop
```

---

## 2. Configurar (opcional)

```bash
cd infra/apps
cp .env.example .env   # ajusta credenciales si quieres
```

Variables relevantes (con valores por defecto):

| Variable | Default | Uso |
|---|---|---|
| `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | `pedidos360` / `postgres` / `postgres` | PostgreSQL |
| `RABBITMQ_ERLANG_COOKIE` | `pedidos360-erlang-cookie` | cookie compartida del clúster |
| `KEYCLOAK_ADMIN_USERNAME` / `KEYCLOAK_ADMIN_PASSWORD` | `admin` / `admin` | admin console de Keycloak |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8090/realms/pedidos360` | emisor del JWT |
| `KEYCLOAK_JWK_SET_URI` | `http://keycloak:8080/realms/pedidos360/protocol/openid-connect/certs` | claves públicas (interno) |
| `KEYCLOAK_AUDIENCE` | `pedidos360-spa` | audience esperado |

---

## 3. Levantar todo

```bash
cd infra/apps
docker compose up -d --build
```

El primer build compila los 6 JARs y el bundle Angular (puede tardar varios minutos).

---

## 4. Verificar el estado

```bash
docker compose ps
```

Todos los servicios deben quedar `healthy` (o `running`), en especial `rabbitmq1`,
`rabbitmq2`, `postgres` y `keycloak`.

```bash
docker compose logs -f            # logs en vivo
docker compose logs consumer-service   # logs del consumidor (ACK/NACK)
```

---

## 5. Verificar el clúster RabbitMQ (2 nodos)

```bash
docker compose exec rabbitmq1 rabbitmqctl cluster_status
# Debe listar rabbit@rabbitmq1 y rabbit@rabbitmq2 (running_nodes: 2)
```

Management UI: `http://localhost:15672` (`guest` / `guest`).

---

## 6. Verificar Keycloak

- Admin console: `http://localhost:8090` (`admin` / `admin`).
- Realm `pedidos360`, cliente `pedidos360-spa`, usuarios `admin` / `operador` / `cliente`
  (ver contraseñas en `docs/BITACORA.md`).

---

## 7. Verificar el flujo completo (resumen)

```bash
# 1) Token de Keycloak
TOKEN=$(curl -s -X POST "http://localhost:8090/realms/pedidos360/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=pedidos360-spa" -d "grant_type=password" \
  -d "username=admin" -d "password=admin123" | jq -r .access_token)

# 2) Gateway con token -> 200
curl -i http://localhost:8080/api/pedidos -H "Authorization: Bearer $TOKEN"

# 3) Gateway sin token -> 401
curl -i http://localhost:8080/api/pedidos

# 4) Crear pedido (publica evento -> consumidor ACK)
curl -i -X POST http://localhost:8080/api/pedidos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"cliente":"Demo","items":[{"productoId":1,"cantidad":2}]}'
```

> El detalle completo (incluida la demostración del desvío a DLQ) está en
> **`docs/GUION_DEMO_EP4.md`**.

---

## 8. Detener / limpiar

```bash
docker compose down            # detiene y elimina contenedores (conserva el volumen de Postgres)
docker compose down -v         # además elimina los volúmenes (datos)
```

---

## 9. Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| `rabbitmq2` no queda `healthy` | no se unió al clúster | `docker compose down && docker compose up -d`; si persiste, join manual (abajo) |
| Gateway devuelve 401 con token válido | issuer/audience mal configurado | revisar `KEYCLOAK_ISSUER_URI` / `KEYCLOAK_AUDIENCE` en `.env` |
| Frontend en blanco | Keycloak no arrancó / redirect URI | revisar `docker compose logs keycloak` y `redirectUris` del realm |
| Keycloak no marca `healthy` | tarda ~30 s en iniciar | esperar; el clúster y la demo siguen operativos |

**Join manual del clúster (solo si falla el automático):**

```bash
docker compose exec rabbitmq2 rabbitmqctl stop_app
docker compose exec rabbitmq2 rabbitmqctl join_cluster rabbit@rabbitmq1
docker compose exec rabbitmq2 rabbitmqctl start_app
```
