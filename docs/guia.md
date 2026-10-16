# Guía de Despliegue y Pruebas — Entorno Docker Local

Esta guía explica cómo levantar y probar **Pedidos360** de forma 100 % local
(Keycloak como IDaaS + Spring Cloud Gateway + microservicios Spring Boot +
Angular + clúster RabbitMQ de 2 nodos) sin depender de Azure AD ni de AWS.

---

## 1. Requisitos

| Herramienta | Versión mínima | Notas |
|---|---|---|
| Docker Desktop | 24+ | Con **Docker Compose v2** (`docker compose`). |
| Git | 2.x | Para clonar el repo. |
| Navegador | — | Chrome/Edge/Firefox recientes. |

> ⚠️ **No se requiere instalar Java, Maven ni Node en el host.** Todos los
> microservicios y el frontend se compilan dentro de contenedores con
> *multi-stage builds* (`maven:3.9-eclipse-temurin-21` y `node:20-alpine`).

Verifica que Docker esté corriendo:

```bash
docker info
docker compose version
```

---

## 2. Clonar y ubicarse en la rama correcta

```bash
git clone https://github.com/victor99a/Cloud-Native.git
cd Cloud-Native
git checkout develop
git pull origin develop
```

> La rama principal de integración es **`develop`**. El flujo es
> `feature/* → develop → main`.

---

## 3. Configurar las variables de entorno

El archivo `.env` (en `infra/apps/`) ya trae valores por defecto funcionales.
Si no existe, cópialo desde el ejemplo:

```bash
cd infra/apps
cp .env.example .env
```

Contenido esperado (valores locales por defecto):

```ini
DB_NAME=pedidos360
DB_USERNAME=postgres
DB_PASSWORD=postgres

RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_ERLANG_COOKIE=pedidos360-erlang-cookie

KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin

KEYCLOAK_ISSUER_URI=http://localhost:8090/realms/pedidos360
KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/pedidos360/protocol/openid-connect/certs
KEYCLOAK_AUDIENCE=pedidos360-spa
```

> **Importante:** `KEYCLOAK_ISSUER_URI` usa la URL que ve el **navegador**
> (`localhost:8090`), mientras que `KEYCLOAK_JWK_SET_URI` usa la URL
> **interna** del contenedor (`keycloak:8080`). No las intercambies.

---

## 4. Levantar el entorno

Desde `infra/apps`:

```bash
docker compose up --build -d
```

Esto construye las imágenes (6 microservicios + frontend) y levanta:

| Contenedor | Puerto(s) |
|---|---|
| `postgres` | `5433:5432` |
| `rabbitmq1` | `5672:5672`, `15672:15672` |
| `rabbitmq2` | `5673:5672`, `15673:15672` |
| `keycloak` | `8090:8080` |
| `pedidos-service` | `8082:8081` |
| `productos-service` | `8083:8082` |
| `consumer-service` | `8084:8083` |
| `admin-service` | `8085:8084` |
| `bff` | `8081:8080` |
| `gateway` | `8080:8080` |
| `frontend` | `4200:80` |

> ⚠️ El primer build puede tardar varios minutos (descarga de dependencias
> Maven y npm). Los builds siguientes usan la caché de capas y son rápidos.

Espera a que todos los contenedores estén sanos:

```bash
docker compose ps
```

Debes ver `(healthy)` en `postgres`, `rabbitmq1`, `rabbitmq2`, `keycloak` y
`frontend`.

> ⚠️ **Nota sobre el primer arranque:** `rabbitmq1` puede reportarse
> "unhealthy" unos instantes mientras RabbitMQ termina de bootear (el clúster
> tarda en estar listo). Si los servicios dependientes quedan en estado
> `Created`, vuelve a ejecutar `docker compose up -d` una vez que
> `rabbitmq1` esté `(healthy)`.

---

## 5. Accesos y credenciales

| Recurso | URL | Usuario / Contraseña |
|---|---|---|
| **Frontend (SPA)** | http://localhost:4200 | login con cuentas del realm (abajo) |
| **API Gateway** (único punto de entrada) | http://localhost:8080 | requiere JWT (`Authorization: Bearer`) |
| **Keycloak admin console** | http://localhost:8090 | `admin` / `admin` |
| **RabbitMQ Management** | http://localhost:15672 | `guest` / `guest` |
| **RabbitMQ Management (nodo 2)** | http://localhost:15673 | `guest` / `guest` |
| **PostgreSQL** | localhost:5433 | `postgres` / `postgres` (db `pedidos360`) |

### Usuarios del realm `pedidos360`

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `operador` | `operador123` | `OPERADOR` |
| `cliente` | `cliente123` | `CLIENTE` |

---

## 6. Verificación rápida del entorno

### 6.1. Salud de la API

```bash
# Sin token → debe devolver 401
curl -i http://localhost:8080/api/pedidos

# Healthcheck público del Gateway
curl http://localhost:8080/actuator/health
```

### 6.2. Clúster RabbitMQ (2 nodos)

```bash
docker exec infra-apps-rabbitmq1-1 rabbitmqctl cluster_status
```

> Debe mostrar **2 nodos** (`rabbit@rabbitmq1` y `rabbit@rabbitmq2`) en
> `Running Nodes` y `Disk Nodes`.

### 6.3. Colas / exchanges / DLQs

En la consola http://localhost:15672 (guest/guest) debes ver:

- **Colas de negocio:** `pedidos360.notificaciones.queue`,
  `pedidos360.tickets.queue`, `pedidos360.documentos.queue`.
- **DLQs:** `pedidos360.notificaciones.dlq`, `pedidos360.tickets.dlq`,
  `pedidos360.documentos.dlq`.
- **Exchanges:** `pedidos360.topic` (topic), `pedidos360.direct` (direct),
  `pedidos360.dlx` (direct).

### 6.4. Realm de Keycloak importado

```bash
curl http://localhost:8090/realms/pedidos360/.well-known/openid-configuration
```

Debe devolver **HTTP 200** con la configuración OIDC del realm.

---

## 7. Pruebas end-to-end (checklist)

### 7.1. Obtener un token (password grant, solo para pruebas)

```bash
curl -X POST "http://localhost:8090/realms/pedidos360/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "grant_type=password&client_id=pedidos360-spa&username=admin&password=admin123&scope=openid"
```

Copia el valor de `access_token` y úsalo como `TOKEN` en los siguientes pasos.

### 7.2. Crear un producto

```bash
curl -X POST "http://localhost:8080/api/productos" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sku":"SKU-001","nombre":"Café","descripcion":"Café de grano","precio":9.99,"stock":50,"activo":true}'
```

### 7.3. Crear un pedido (productor → RabbitMQ)

```bash
curl -X POST "http://localhost:8080/api/pedidos" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Juan Pérez","items":[{"productoId":1,"cantidad":2}]}'
```

### 7.4. Verificar el flujo productor → consumidor

```bash
docker logs infra-apps-consumer-service-1 --tail 50
```

Debes ver que `NotificationConsumer` (topic `pedido.#`) y `TicketConsumer`
(direct `pedido.creado`) reciben el mensaje y responden `PROCESADO_OK`.

### 7.5. Avanzar el estado de un pedido

```bash
curl -X PATCH "http://localhost:8080/api/pedidos/1/estado" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"estado":"CONFIRMADO"}'
```

Cuando el pedido llegue a `ENTREGADO`, el `DocumentosConsumer`
(topic `pedido.estado.entregado`) lo procesará.

### 7.6. Probar la DLQ

Publica un mensaje con un payload inválido (o simula un fallo del consumidor)
y verifica que el mensaje aparece en la DLQ correspondiente en la consola de
RabbitMQ.

### 7.7. Admin REST (administración de RabbitMQ)

```bash
curl -X POST http://localhost:8085/api/v1/admin/queues \
  -H "Content-Type: application/json" \
  -d '{"name":"demo.queue"}'

curl -X DELETE http://localhost:8085/api/v1/admin/queues/demo.queue

curl -X POST http://localhost:8085/api/v1/admin/exchanges \
  -H "Content-Type: application/json" \
  -d '{"name":"demo.exchange","type":"direct"}'

curl -X POST http://localhost:8085/api/v1/admin/bindings \
  -H "Content-Type: application/json" \
  -d '{"queue":"demo.queue","exchange":"demo.exchange","routingKey":"demo"}'
```

> Un nombre vacío debe devolver **HTTP 400**.

### 7.8. Login por UI

Abre http://localhost:4200, inicia sesión con `admin` / `admin123` y verifica
que el dashboard carga pedidos y productos.

---

## 8. Solución de problemas (troubleshooting)

| Síntoma | Causa probable | Solución |
|---|---|---|
| `rabbitmq1` queda `unhealthy` y los servicios dependientes en `Created` | RabbitMQ tardó en bootear más que el retry del healthcheck | Espera a que esté `healthy` y vuelve a `docker compose up -d` |
| `keycloak` nunca pasa a `healthy` | El healthcheck de Keycloak 25 vive en el *management interface* (puerto 9000), no en el 8080 | Ya está corregido en el `docker-compose.yml` |
| El frontend no carga datos (o la red muestra 404) | Las llamadas van a `/pedidos` en vez de `/api/pedidos` | Ya está corregido en `ApiService` (baseUrl incluye `/api`) |
| El navegador bloquea las llamadas por CORS | El preflight `OPTIONS` era rechazado con 401 | Ya está corregido en `CorsConfig` (`@Order(HIGHEST_PRECEDENCE)`) |
| El backend rechaza un token válido con 401 | El token no traía `aud=pedidos360-spa` | Ya está corregido con el `oidc-audience-mapper` en el realm |
| El Gateway falla al arrancar (`ReactiveJwtDecoder`) | Se usaba `JwtDecoder` (bloqueante) en una app reactiva | Ya está corregido (`NimbusReactiveJwtDecoder`) |
| `NoSuchMethodError: HttpHeaders.headerSet()` | Spring Cloud Gateway 4.1.6 requiere Spring Framework 6.1.15+ | Ya está corregido (Spring Boot `3.3.6`) |

### Ver logs de un servicio

```bash
docker compose logs -f <servicio>
# ejemplos: gateway, bff, pedidos-service, consumer-service, keycloak
```

### Reconstruir un servicio tras cambiar código

```bash
docker compose up -d --build <servicio>
```

---

## 9. Detener, reiniciar y limpiar

```bash
# Detener (mantiene volúmenes)
docker compose down

# Detener y eliminar el volumen de datos de PostgreSQL (borra la data)
docker compose down -v

# Reconstruir todo desde cero (sin caché)
docker compose build --no-cache
docker compose up -d
```

---

## 10. Topología de mensajería (referencia)

| Cola | Binding | DLQ |
|---|---|---|
| `pedidos360.notificaciones.queue` | topic `pedido.#` | `pedidos360.notificaciones.dlq` |
| `pedidos360.tickets.queue` | direct `pedido.creado` | `pedidos360.tickets.dlq` |
| `pedidos360.documentos.queue` | topic `pedido.estado.entregado` | `pedidos360.documentos.dlq` |

**Exchanges:** `pedidos360.topic` (topic), `pedidos360.direct` (direct),
`pedidos360.dlx` (dead-letter, direct).

Los consumidores usan **ACK manual**: `basicAck` en éxito y
`basicNack(requeue=false)` en fallo irrecuperable → el mensaje se desvía a su
DLQ.
