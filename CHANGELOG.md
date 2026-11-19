# Changelog — Pedidos360

## Sin publicar

### Mensajería (P1)
- RabbitMQ: publisher confirms y returns activados de verdad (`publisher-confirm-type: correlated`,
  `publisher-returns: true`); antes los callbacks del `RabbitTemplate` nunca se ejecutaban.
- RabbitMQ: los servicios conocen los 2 nodos del clúster (`RABBITMQ_ADDRESSES`) y reparten sus
  conexiones entre ellos (`address-shuffle-mode: random`); si un nodo cae, reconectan al otro.
  Limitación: las colas son *classic* y residen en `rabbitmq1`; para que sobrevivan a la caída de un
  nodo se necesitan colas quorum, que requieren al menos 3 nodos.
- Consumidores RabbitMQ: un error transitorio se reintenta una sola vez; si el mensaje ya fue reentregado
  va a la DLQ (antes podía reencolarse para siempre).
- Eventos de pedido publicados solo después del commit de la transacción (no se anuncian pedidos revertidos).
- `pedidos360.analitica.metricas`: `cleanup.policy=compact,delete` para que `retention.ms` (30 días) aplique;
  `kafka-init` alinea la configuración también en clústeres ya existentes.

## v1.0.0 — 2026-11-19

Primera versión estable, desplegada y validada en AWS (EP5 + EP6).

### Nube (AWS)
- API Manager: Amazon API Gateway (HTTP API) con authorizer JWT de Cognito en `/api/*`.
- IDaaS: Amazon Cognito (`pedidos360-tenant`, usuarios admin/operador/cliente por grupo).
- Despliegue idempotente con `infra/aws/deploy.sh` (y `destroy.sh`) en 2 EC2 (mensajería y apps).
- Frontend con configuración OIDC en tiempo de ejecución (`/config.json`): la misma imagen sirve
  para cualquier cuenta o tenant.

### Mensajería
- RabbitMQ: clúster de 2 nodos, 3 colas + 3 DLQ, exchanges direct/topic/dlx.
  El administrador ahora también **elimina bindings** (`DELETE /api/v1/admin/bindings`).
- Kafka: 3 Zookeeper + 3 brokers, tópicos `pedidos360.pedidos.eventos` y
  `pedidos360.analitica.metricas` (3 particiones × 3 réplicas) + DLT, Kafka UI,
  administrador programático (tópicos, particiones, consumer groups, offsets y lag).

### Analítica (corrección)
- Cada pedido cuenta **una sola vez**, en su estado actual: un cambio de estado resta 1 al
  estado anterior y suma 1 al nuevo, una eliminación descuenta y los eventos repetidos
  (mismo `eventId`) se ignoran. Antes, cada evento sumaba de nuevo e inflaba los totales.
- `PedidoEvent` incorpora `eventId`, `tipoEvento` (`PEDIDO_CREADO`, `ESTADO_ACTUALIZADO`,
  `PEDIDO_ELIMINADO`) y `ocurridoEn`.
- Alertas: el alto valor se alerta solo al crear el pedido; las eliminaciones no alertan.

### Calidad
- 46 pruebas automatizadas en los 8 microservicios (`mvn clean verify`).

### Pendientes conocidos (no bloquean la versión)
- (Resueltos en la siguiente versión: confirms, nodos, reintentos, after-commit y política del tópico.)
- Restringir el puerto 8080 de la EC2 de apps al API Gateway (VPC Link).
- La numeración `PED-NNNNNN` usa `count()+1` y puede repetirse tras eliminar pedidos intermedios.
