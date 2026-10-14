# Cloud-Native — Pedidos360

Proyecto **Pedidos360** — arquitectura Cloud Native (Angular + Spring Boot microservicios + RabbitMQ + Azure AD/Entra ID + AWS).

## Arquitectura (monorepo)

| Componente | Tecnología | Puerto | Descripción |
|-----------|-----------|--------|-------------|
| `pedidos360-frontend/` | Angular 18 (standalone, MSAL) | 4200 | SPA con login OIDC (Authorization Code + PKCE) |
| `pedidos360-bff/` | Spring Boot 3.3 (Java 21) | 8080 | Backend-For-Frontend: valida JWT y enruta a los servicios |
| `pedidos360-pedidos-service/` | Spring Boot 3.3 (Java 21) | 8081 | CRUD de pedidos + **productor** de eventos RabbitMQ |
| `pedidos360-productos-service/` | Spring Boot 3.3 (Java 21) | 8082 | CRUD de productos |
| `pedidos360-consumer-service/` | Spring Boot 3.3 (Java 21) | 8083 | **Consumidores** RabbitMQ (notificaciones, tickets, documentos) |
| `pedidos360-admin-service/` | Spring Boot 3.3 (Java 21) | 8084 | **Administrador** RabbitMQ (REST para colas/exchanges/bindings) |
| `infra/apps/` | Docker Compose | — | PostgreSQL + clúster RabbitMQ (2 nodos) + servicios |

## Topología RabbitMQ (mensajería desacoplada)

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

## Requisitos

- Java 21, Maven 3.9+, Node 18+, Docker + Docker Compose.
- Credenciales Azure AD (App registration "Pedidos360") para el login.

## Ejecutar pruebas

```bash
# Backend (cada microservicio)
mvn test -f pedidos360-pedidos-service/pom.xml
mvn test -f pedidos360-productos-service/pom.xml
mvn test -f pedidos360-bff/pom.xml

# Frontend
cd pedidos360-frontend && npm install && npm run build
```

## Levantar el entorno (docker-compose)

```bash
cd infra/apps
cp .env.example .env      # completar AZURE_ISSUER_URI y AZURE_AUDIENCE reales
docker compose up --build
```

Esto levanta: PostgreSQL, **clúster RabbitMQ de 2 nodos** (`rabbitmq1` + `rabbitmq2` con la
misma `RABBITMQ_ERLANG_COOKIE` y `rabbitmq_management`), y los 5 microservicios.

## Verificación en vivo (checklist EP3/EP4)

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
6. **Login IDaaS** → abrir el frontend y loguearse con cuenta institucional (Azure AD).
7. **JWT en backend** → petición sin token al BFF → HTTP 401; con token válido → 200.

## Modelo de ramas

`feature/* → develop → main`. Ver `CONTRIBUTING.md`.
