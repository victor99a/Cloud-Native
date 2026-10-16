# Guion de Demostración en Vivo — EP4 (Infraestructura Local con Docker Compose)

> **Objetivo:** exhibir ante el evaluador el 100% de los criterios EP3/EP4 sobre una
> infraestructura **100% local** (sin AWS/Azure/GCP), orquestada con Docker Compose.
>
> **Stack:** Angular 18 + 6 microservicios Spring Boot 3.3 (Java 21) + API Gateway
> (Spring Cloud Gateway) + Keycloak (IDaaS) + RabbitMQ (clúster 2 nodos) + PostgreSQL.

---

## 0. Mapa de puertos (todo en `localhost`)

| Componente | URL / Puerto | Rol |
|---|---|---|
| **Gateway** (punto único de entrada) | `http://localhost:8080` | Valida JWT y enruta al BFF |
| **Frontend** (nginx) | `http://localhost:4200` | SPA Angular |
| **Keycloak** (IDaaS) | `http://localhost:8090` | Login SSO + emisor de JWT |
| **BFF** | `http://localhost:8081` (debug) | Orquesta pedidos/productos |
| **pedidos-service** (productor) | `http://localhost:8082` (debug) | CRUD pedidos + publica eventos |
| **productos-service** | `http://localhost:8083` (debug) | CRUD productos |
| **consumer-service** | `http://localhost:8084` (debug) | Consumidores RabbitMQ |
| **admin-service** | `http://localhost:8085` | Administrador RabbitMQ (REST) |
| **RabbitMQ nodo 1** | `15672` / `5672` | Clúster (management UI) |
| **RabbitMQ nodo 2** | `15673` / `5673` | Clúster |
| **PostgreSQL** | `5433` | Persistencia |

**Credenciales de prueba (Keycloak, realm `pedidos360`):**

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `operador` | `operador123` | `OPERADOR` |
| `cliente` | `cliente123` | `CLIENTE` |

> **Nota Windows:** en PowerShell, `curl` es un alias de `Invoke-WebRequest`. Usa
> `curl.exe` (curl real, incluido en Windows 10/11) o ejecuta los comandos desde
> **Git Bash**. En macOS/Linux usa `curl` normal.

---

## PASO 1 — Levantar todo y mostrar servicios `healthy`

```bash
# Desde la carpeta infra/apps (donde está el docker-compose.yml)
cd infra/apps

# (opcional) copia el .env si quieres personalizar credenciales
# cp .env.example .env

# Construir y levantar TODO en segundo plano
docker compose up -d --build

# Ver el estado de todos los servicios (columna STATUS debe quedar en "healthy")
docker compose ps
```

**Qué mostrar al evaluador:** la tabla de `docker compose ps` con todos los servicios
en `healthy` (o `running`), especialmente `rabbitmq1` y `rabbitmq2`.

> El primer `--build` compila los 6 JARs y el bundle Angular; puede tardar unos minutos.
> Mientras compila, puedes ir explicando la arquitectura.

```bash
# (opcional) seguir los logs en vivo para la demo
docker compose logs -f
```

---

## PASO 2 — Clúster RabbitMQ activo (2 nodos)

Abre en el navegador: **`http://localhost:15672`** (usuario `guest`, clave `guest`).

Ve a **Overview → Nodes** y muestra que existen **2 nodos en verde**:

- `rabbit@rabbitmq1`
- `rabbit@rabbitmq2`

También por consola:

```bash
docker compose exec rabbitmq1 rabbitmqctl cluster_status
# Debe listar: rabbit@rabbitmq1 y rabbit@rabbitmq2 (running_nodes: 2)
```

**Qué mostrar:** los dos nodos formando un único clúster (no dos brokers aislados).

---

## PASO 3 — Topología de mensajería (exchanges + 3 colas + 3 DLQ)

En la misma consola `http://localhost:15672`:

- **Exchanges** (pestaña *Exchanges*): `pedidos360.topic` (topic), `pedidos360.direct`
  (direct) y `pedidos360.dlx` (direct, usado como *dead-letter exchange*).
- **Colas** (pestaña *Queues*): las 3 de negocio **y** sus 3 DLQ:

| Cola de negocio | Dead Letter Queue |
|---|---|
| `pedidos360.notificaciones.queue` | `pedidos360.notificaciones.dlq` |
| `pedidos360.tickets.queue` | `pedidos360.tickets.dlq` |
| `pedidos360.documentos.queue` | `pedidos360.documentos.dlq` |

**Qué mostrar:** que cada cola de negocio declara `x-dead-letter-exchange =
pedidos360.dlx` (ver *Features/Arguments* de cada cola) y que existen las 3 DLQ.

---

## PASO 4 — Validación de JWT en el Gateway (200 vs 401)

El Gateway (`:8080`) es el punto único de entrada y valida el JWT **antes** de enrutar.

### 4.1 Obtener un token válido de Keycloak

```bash
# Password grant (direct access) contra Keycloak local
curl.exe -s -X POST "http://localhost:8090/realms/pedidos360/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=pedidos360-spa" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123"
```

Copia el valor de `access_token` de la respuesta JSON (o usa `jq` si está disponible):

```bash
TOKEN=$(curl.exe -s -X POST "http://localhost:8090/realms/pedidos360/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=pedidos360-spa" -d "grant_type=password" \
  -d "username=admin" -d "password=admin123" | jq -r .access_token)
```

> En PowerShell sin `jq`, guarda la respuesta y extrae el token, o ejecuta los `curl`
> desde Git Bash.

### 4.2 Petición **con token válido** → `200 OK`

```bash
curl.exe -i http://localhost:8080/api/pedidos \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** `HTTP/1.1 200 OK` con el listado (posiblemente `[]`).

### 4.3 Petición **sin token** → `401 Unauthorized`

```bash
curl.exe -i http://localhost:8080/api/pedidos
```

**Resultado esperado:** `HTTP/1.1 401 Unauthorized` (bloqueado por el Gateway).

### 4.4 Petición **con token manipulado** → `401 Unauthorized`

```bash
curl.exe -i http://localhost:8080/api/pedidos \
  -H "Authorization: Bearer este.es.un.token.manipulado"
```

**Resultado esperado:** `HTTP/1.1 401 Unauthorized` (firma/issuer inválido).

**Qué mostrar:** que el **Gateway** (no el BFF ni el frontend) rechaza la petición
antes de llegar a los microservicios. Puedes corroborar con
`docker compose logs gateway` (log de Spring Security rechazando el token).

---

## PASO 5 — Flujo de mensajería y desvío a DLQ

### 5.1 Flujo feliz (publicación → consumo → **ACK**)

1. Crea un pedido a través del **Gateway** (con el token de `admin`):

```bash
curl.exe -i -X POST http://localhost:8080/api/pedidos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Cliente Demo","items":[{"productoId":1,"cantidad":2}]}'
```

2. Observa en vivo los logs del consumidor (deja otra terminal):

```bash
docker compose logs -f consumer-service
```

**Qué mostrar:** el log del `consumer-service` con:

```
[notificaciones] RECIBIDO deliveryTag=1 payload={...}
[notificaciones] PROCESADO_OK deliveryTag=1      <- basicAck
[tickets] RECIBIDO deliveryTag=1 payload={...}
[tickets] PROCESADO_OK deliveryTag=1              <- basicAck
```

Es decir: el productor publica `pedido.creado`, las colas de negocio lo enrutan y los
consumidores confirman con `basicAck`.

### 5.2 Fallo intencional → **NACK sin requeue** → **DLQ**

Provoca un mensaje irrecuperable publicando un payload **vacío** directamente en la cola
de notificaciones (el consumidor valida que no esté vacío):

```bash
curl.exe -u guest:guest -X POST \
  "http://localhost:15672/api/exchanges/%2F/amq.default/publish" \
  -H "Content-Type: application/json" \
  -d '{"properties":{"delivery_mode":2},"routing_key":"pedidos360.notificaciones.queue","payload":"","payload_encoding":"string"}'
```

Observa de nuevo el log del consumidor:

```bash
docker compose logs -f consumer-service
```

**Qué mostrar:**

```
[notificaciones] RECIBIDO deliveryTag=2 payload=
[notificaciones] FALLO irrecuperable -> DLQ deliveryTag=2 error=payload vacío   <- basicNack(requeue=false)
```

Y en la consola de RabbitMQ (`http://localhost:15672` → *Queues*):

- `pedidos360.notificaciones.queue` queda en **0 mensajes** (no se reencola).
- `pedidos360.notificaciones.dlq` **incrementa** su contador (el mensaje se desvió).

**Qué mostrar:** que el `basicNack(..., requeue=false)` desvía el mensaje a la DLQ a
través del `x-dead-letter-exchange` (`pedidos360.dlx`), **sin** bucle de redelivery.

---

## PASO 6 (opcional) — Login por navegador (Frontend + Keycloak)

1. Abre `http://localhost:4200`.
2. Pulsa **Iniciar sesión** → redirige a **Keycloak** (`http://localhost:8090`).
3. Entra con `admin` / `admin123` → vuelve al dashboard autenticado.
4. El SPA llama al Gateway (`http://localhost:8080/api/...`) adjuntando el Bearer token.

---

## Anexo — Administrador RabbitMQ (EP3) y validación de DTOs

El `admin-service` expone endpoints REST para crear/eliminar colas, exchanges y bindings:

```bash
# Crear cola (válido) -> 201
curl.exe -X POST http://localhost:8085/api/v1/admin/queues \
  -H "Content-Type: application/json" \
  -d '{"name":"demo.queue"}'

# Crear cola con nombre inválido -> 400 (validación @Pattern)
curl.exe -i -X POST http://localhost:8085/api/v1/admin/queues \
  -H "Content-Type: application/json" \
  -d '{"name":"nombre inválido!!"}'

# Eliminar cola -> 204
curl.exe -X DELETE http://localhost:8085/api/v1/admin/queues/demo.queue

# Crear exchange (topic) -> 201
curl.exe -X POST http://localhost:8085/api/v1/admin/exchanges \
  -H "Content-Type: application/json" \
  -d '{"name":"demo.exchange","type":"topic"}'

# Crear binding -> 201
curl.exe -X POST http://localhost:8085/api/v1/admin/bindings \
  -H "Content-Type: application/json" \
  -d '{"queue":"pedidos360.notificaciones.queue","exchange":"demo.exchange","routingKey":"demo.#"}'
```

---

## Anexo — Verificación técnica (sin Docker, opcional)

Para compilar localmente sin levantar contenedores:

```bash
# Backend (6 servicios)
mvn -DskipTests package -f pedidos360-gateway/pom.xml
mvn -DskipTests package -f pedidos360-bff/pom.xml
mvn -DskipTests package -f pedidos360-pedidos-service/pom.xml
mvn -DskipTests package -f pedidos360-productos-service/pom.xml
mvn -DskipTests package -f pedidos360-consumer-service/pom.xml
mvn -DskipTests package -f pedidos360-admin-service/pom.xml

# Frontend
cd pedidos360-frontend && npm install && npm run build
```

---

## Anexo — Solución de problemas comunes

- **Keycloak healthcheck no marca `healthy`:** el contenedor tarda ~30 s en iniciar.
  Verifica `docker compose logs keycloak`; si el healthcheck `/dev/tcp` no funciona en
  tu imagen, el clúster y la demo siguen operativos (solo cambia el estado a `running`).
- **`aud` del token:** el JWT emitido para `pedidos360-spa` lleva `aud = pedidos360-spa`.
  El BFF y el Gateway validan exactamente ese audience.
- **Issuer vs hostname interno:** el navegador ve Keycloak en `localhost:8090` (issuer),
  pero los contenedores lo alcanzan en `keycloak:8080` (JWKS). Por eso BFF/Gateway usan
  `jwk-set-uri` interno + validación explícita de `issuer`.
- **Clúster no se une:** reinicia con `docker compose down && docker compose up -d`; si
  persiste, join manual (ver `docs/RUNBOOK_DESPLIEGUE.md`).
