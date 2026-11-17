# Cloud-Native — Pedidos360

Proyecto **Pedidos360** — arquitectura **Cloud Native** (Angular + Spring Boot microservicios + RabbitMQ + Apache Kafka + Keycloak/IDaaS), orquestada **100% local** con Docker Compose.

---

## 🧭 Vista general de la arquitectura

```mermaid
flowchart TB
    subgraph Frontend["Frontend (Angular 18)"]
        SPA["SPA · login OIDC<br/>Authorization Code + PKCE"]
    end

    subgraph IDaaS["IDaaS (Keycloak)"]
        KC["Realm pedidos360<br/>emite el JWT"]
    end

    subgraph Perimetro["Perímetro de seguridad"]
        GW["API Gateway<br/>Spring Cloud Gateway<br/>valida el JWT"]
    end

    subgraph Backend["Microservicios (Spring Boot 3.3 / Java 21)"]
        BFF["BFF<br/>valida JWT + roles"]
        PED["pedidos-service<br/>CRUD + productor"]
        PROD["productos-service<br/>CRUD"]
        CONS["consumer-service<br/>RabbitMQ"]
        ADMIN["admin-service<br/>RabbitMQ"]
        ANALYT["analytics-service<br/>Kafka"]
        KADMIN["kafka-admin-service"]
    end

    subgraph Mensajeria["Mensajería (eventos)"]
        RAB[("RabbitMQ<br/>2 nodos<br/>3 colas + 3 DLQ")]
        KAF[("Kafka<br/>3 ZK + 3 brokers<br/>2 tópicos")]
    end

    DB[("PostgreSQL")]

    SPA -->|"Authorization: Bearer <JWT>"| GW
    KC -.->|"entrega el token"| SPA
    GW -->|"rutas /api/**"| BFF
    BFF --> PED
    BFF --> PROD
    BFF --> ANALYT
    PED -->|"eventos de dominio"| RAB
    PED -->|"eventos de dominio"| KAF
    RAB --> CONS
    KAF --> ANALYT
    ANALYT --> DB
    PED --> DB
    PROD --> DB
```

### Flujo de una petición (de izq. a der.)

1. **Login** → el SPA autentica contra Keycloak (OAuth 2.0 / OIDC, *Authorization Code + PKCE*) y obtiene un **JWT**.
2. **Llamada a la API** → el SPA envía el JWT en `Authorization: Bearer` a través del **API Gateway**.
3. **API Gateway** → valida firma / issuer / audience / expiración. Token inválido → **401/403**; válido → reenvía al **BFF**.
4. **BFF** → re-valida el JWT (defensa en profundidad), extrae los roles y enruta al microservicio correspondiente.
5. **Eventos** → `pedidos-service` publica eventos de dominio **a la vez** en RabbitMQ (transaccional) y Kafka (streaming/analítica).

---

## 🧩 Componentes (monorepo)

| Componente | Tecnología | Puerto | Descripción |
|-----------|-----------|--------|-------------|
| `pedidos360-frontend/` | Angular 18 (standalone, Keycloak) | 4200 | SPA con login OIDC (Authorization Code + PKCE) |
| `pedidos360-bff/` | Spring Boot 3.3 (Java 21) | 8080 | Backend-For-Frontend: valida JWT y enruta a los servicios |
| `pedidos360-pedidos-service/` | Spring Boot 3.3 (Java 21) | 8081 | CRUD de pedidos + **productor** de eventos RabbitMQ **y** Kafka |
| `pedidos360-productos-service/` | Spring Boot 3.3 (Java 21) | 8082 | CRUD de productos |
| `pedidos360-consumer-service/` | Spring Boot 3.3 (Java 21) | 8083 | **Consumidores** RabbitMQ (notificaciones, tickets, documentos) |
| `pedidos360-admin-service/` | Spring Boot 3.3 (Java 21) | 8084 | **Administrador** RabbitMQ (REST para colas/exchanges/bindings) |
| `pedidos360-analytics-service/` | Spring Boot 3.3 (Java 21) | 8086 | **Analítica/streaming**: consume eventos de pedidos desde Kafka, agrega métricas y publica resultados |
| `pedidos360-kafka-admin-service/` | Spring Boot 3.3 (Java 21) | 8087 | **Administrador** Kafka (REST para tópicos y consumer groups) |
| `infra/apps/` | Docker Compose | — | PostgreSQL + clúster RabbitMQ (2 nodos) + clúster Kafka (3 ZK + 3 brokers) + Kafka UI + servicios |

### ¿Por qué cada tecnología? (para defender la arquitectura)

| Pieza | Rol | Por qué se usa |
|-------|-----|----------------|
| **API Gateway** | Único punto de entrada | Valida el JWT **antes** de tocar el backend; centraliza CORS y enrutamiento. |
| **BFF** | Fachada del frontend | No expone los microservicios directamente; agrega autorización por rol. |
| **Keycloak (IDaaS)** | Identidad | OIDC estándar: el SPA no guarda credenciales, solo un JWT firmado. |
| **RabbitMQ** | Mensajería transaccional | Garantías de entrega (ACK) + DLQ para trabajos tipo notificación/ticket/documento. |
| **Kafka** | Streaming / analítica | Log distribuido, particionado y replicado; ideal para analítica en tiempo real y escalado horizontal. |
| **PostgreSQL** | Persistencia | Estado de negocio (pedidos, productos, métricas). |

---

## 🐰 Topología RabbitMQ (mensajería desacoplada)

Todos los nombres de colas, exchanges, DLX y routing keys están **centralizados** en
`application.yml` (prefijo `pedidos360.rabbitmq`). **Cero hardcoding.**

**Exchanges**
- `pedidos360.topic` (tipo `topic`)
- `pedidos360.direct` (tipo `direct`)
- `pedidos360.dlx` (dead-letter exchange, tipo `direct`)

**Colas de negocio → DLQ**

| Cola | Binding | DLQ |
|------|---------|-----|
| `pedidos360.notificaciones.queue` | topic `pedido.#` | `pedidos360.notificaciones.dlq` |
| `pedidos360.tickets.queue` | direct `pedido.creado` | `pedidos360.tickets.dlq` |
| `pedidos360.documentos.queue` | topic `pedido.estado.entregado` | `pedidos360.documentos.dlq` |

**Flujo:** `pedidos-service` (productor) publica en `pedidos360.topic` (`pedido.creado`,
`pedido.estado.*`) y en `pedidos360.direct` (`pedido.creado`). Los consumidores procesan con
**ACK manual** (`acknowledge-mode: manual`): `basicAck` en éxito y `basicNack(requeue=false)`
en fallo irrecuperable → el mensaje se desvía a su DLQ.

---

## 🚀 Streaming y analítica (Apache Kafka)

Además de RabbitMQ (mensajería transaccional), el sistema publica los eventos de pedidos en
**Kafka** para su procesamiento en streaming y analítica en tiempo real.

**Clúster (tolerancia a fallos / alta disponibilidad)**
- **3 nodos Zookeeper** (`zookeeper1..3`) formando quorum (soporta la caída de 1 nodo).
- **3 brokers Kafka** (`kafka1..3`) con `replication.factor=3` y `min.insync.replicas=2`.
- **Kafka UI** en http://localhost:8091 (brokers, tópicos, particiones, offsets, lag y consumidores).

**Tópicos (3 particiones + 3 réplicas cada uno)**

| Tópico | Uso | retention.ms | cleanup.policy |
|--------|-----|--------------|----------------|
| `pedidos360.pedidos.eventos` | Eventos de dominio (pedido creado / cambio de estado) | 7 días | `delete` |
| `pedidos360.analitica.metricas` | Métricas agregadas por estado | 30 días | `compact` |

**Flujo**
1. `pedidos-service` → productor Kafka → `pedidos360.pedidos.eventos` (key = número de pedido).
2. `analytics-service` (consumer group `pedidos360-analytics-group`) → agrega por estado (PostgreSQL) y
   publica `MetricasPedidoDto` → `pedidos360.analitica.metricas` (key = estado).
3. `analytics-service` (segundo group `pedidos360-reporting-group`) → consume las métricas (reportes/BI).
4. Errores/validación → reintentos (3, backoff 1s) → Dead Letter Topic `pedidos360.pedidos.eventos.dlt`.

La creación de tópicos es doble (idempotente): script `infra/apps/kafka/create-topics.sh`
(contenedor `kafka-init`) y beans `NewTopic` en `analytics-service`.

**Administración programática** — `kafka-admin-service` expone:
- `POST /api/v1/kafka/topics`, `GET /api/v1/kafka/topics`, `GET /api/v1/kafka/topics/{name}`,
  `PUT /api/v1/kafka/topics/{name}/configs`, `DELETE /api/v1/kafka/topics/{name}`.
- `GET /api/v1/kafka/consumer-groups`, `GET /api/v1/kafka/consumer-groups/{groupId}`,
  `DELETE /api/v1/kafka/consumer-groups/{groupId}`.

> ℹ️ La creación de tópicos por defecto usa **3 particiones y 3 réplicas** (tolerancia a fallos
> + paralelismo). Puedes alterar `retention.ms` y `cleanup.policy` con `PUT .../configs`.

Ver `docs/ARQUITECTURA_KAFKA.md` para la explicación completa de tolerancia a fallos,
paralelismo y procesamiento distribuido.

---

## 🔐 Seguridad (perímetro)

La **validación del JWT se concentra en el perímetro**: el **API Gateway** rechaza tokens
inválidos/ausentes/expirados (401/403) y el **BFF** repite la validación (defensa en
profundidad) con control fino por rol (`ADMIN`, `OPERADOR`).

Los microservicios *downstream* (`pedidos`, `productos`, `analytics`) y los *administradores*
(`admin-service` de RabbitMQ, `kafka-admin-service`) **no validan JWT** por diseño: son
componentes internos que solo reciben tráfico del BFF o de la red de Docker Compose. En un
entorno productivo quedarían **detrás del API Manager** o con aislamiento de red; para la
demo local se exponen en puertos de host solo para facilitar la verificación con `curl`.

---

## 📚 Glosario rápido (para la presentación)

| Concepto | En una frase |
|----------|--------------|
| **Exchange** (RabbitMQ) | Router: decide a qué cola(s) va un mensaje según el *routing key* (direct/topic). |
| **Cola / DLQ** | Buzón de mensajes; la **DLQ** recibe los que no se pudieron procesar. |
| **ACK / NACK** | Confirmación manual de procesamiento (éxito / reintento / desvío a DLQ). |
| **Topic** (Kafka) | Canal con nombre donde se publican los mensajes. |
| **Partición** | Subdivisión de un topic: permite **paralelismo** (cada partición la lee un consumidor). |
| **Réplica / ISR** | Copias de una partición entre brokers; garantizan **alta disponibilidad**. |
| **Offset** | Posición del mensaje dentro de su partición; cada consumer group guarda el suyo. |
| **Consumer group** | Grupo de consumidores que comparte el trabajo y recuerda el último offset leído. |
| **Key** | Decide la partición: `hash(key) % nº particiones`; misma key → misma partición → orden garantizado. |
| **retention.ms** | Cuánto tiempo se conservan los mensajes antes de eliminarse/compactarse. |
| **cleanup.policy** | `delete` (borra por tiempo) o `compact` (conserva el último valor por clave). |

---

## ✅ Requisitos

- Java 21, Maven 3.9+, Node 18+, Docker + Docker Compose.
- Keycloak (IDaaS local): el realm `pedidos360` se importa automáticamente al arrancar.

## 🧪 Ejecutar pruebas

```bash
# Backend (cada microservicio)
mvn test -f pedidos360-pedidos-service/pom.xml
mvn test -f pedidos360-productos-service/pom.xml
mvn test -f pedidos360-bff/pom.xml
mvn test -f pedidos360-analytics-service/pom.xml
mvn test -f pedidos360-kafka-admin-service/pom.xml

# Frontend
cd pedidos360-frontend && npm install && npm run build
```

## 🐳 Levantar el entorno (docker-compose)

```bash
cd infra/apps
cp .env.example .env      # los valores por defecto ya funcionan en local
docker compose up --build
```

Esto levanta: PostgreSQL, **clúster RabbitMQ de 2 nodos** (`rabbitmq1` + `rabbitmq2` con la
misma `RABBITMQ_ERLANG_COOKIE` y `rabbitmq_management`), **clúster Kafka de 3 Zookeeper +
3 brokers** (+ Kafka UI en http://localhost:8091), y los 7 microservicios + gateway + frontend.

## 🔎 Verificación en vivo (checklist RabbitMQ)

1. **Clúster 2 nodos** → `docker exec infra-apps-rabbitmq1-1 rabbitmq-diagnostics cluster_status`
   (debe mostrar 2 nodos). Consola: http://localhost:15672 (guest/guest).
2. **3 colas + 3 DLQ + exchanges direct/topic** → Management UI → Queues/Exchanges.
3. **Productor→consumidor** → `POST /api/pedidos` (vía BFF) y ver el log del `consumer-service`.
4. **DLQ** → publicar un mensaje inválido y verificar que aparece en la DLQ correspondiente.
5. **Admin REST** →
   ```bash
   curl -X POST http://localhost:8084/api/v1/admin/queues -H "Content-Type: application/json" -d '{"name":"demo.queue"}'
   curl -X DELETE http://localhost:8084/api/v1/admin/queues/demo.queue
   curl -X POST http://localhost:8084/api/v1/admin/exchanges -H "Content-Type: application/json" -d '{"name":"demo.exchange","type":"direct"}'
   curl -X POST http://localhost:8084/api/v1/admin/bindings -H "Content-Type: application/json" -d '{"queue":"demo.queue","exchange":"demo.exchange","routingKey":"demo"}'
   ```
   (nombre vacío → HTTP 400).
6. **Login IDaaS** → abrir el frontend y loguearse con cuenta institucional (Keycloak).
7. **JWT en backend** → petición sin token al BFF → HTTP 401; con token válido → 200.

## 🔎 Verificación en vivo (checklist Kafka)

1. **Clúster** → `docker exec infra-apps-kafka1-1 kafka-topics --bootstrap-server localhost:29092 --list`
   (debe mostrar los 2 tópicos). Kafka UI: http://localhost:8091.
2. **Tópicos (3 particiones / 3 réplicas)** →
   `docker exec infra-apps-kafka1-1 kafka-topics --bootstrap-server localhost:29092 --describe`.
3. **Streaming** → `POST /api/pedidos` (vía BFF) y ver en los logs de `analytics-service` el evento consumido
   y la métrica publicada; consultar `GET /api/analitica/resumen`.
4. **DLT** → publicar un evento inválido y verificar que llega a `pedidos360.pedidos.eventos.dlt` (Kafka UI).
5. **Admin Kafka** →
   ```bash
   curl -X POST http://localhost:8087/api/v1/kafka/topics -H "Content-Type: application/json" \
     -d '{"name":"demo.topic","partitions":3,"replicas":3}'
   curl -X GET http://localhost:8087/api/v1/kafka/topics
   curl -X DELETE http://localhost:8087/api/v1/kafka/topics/demo.topic
   curl -X GET http://localhost:8087/api/v1/kafka/consumer-groups
   ```

## 🌿 Modelo de ramas

`feature/* → develop → main`. Ver `CONTRIBUTING.md`.
