# Arquitectura Kafka — Pedidos360 (streaming y analítica)

Este documento explica cómo la configuración de Kafka resuelve los requerimientos de
**tolerancia a fallos**, **paralelismo** y **procesamiento distribuido**, y cómo se
integran productores, consumidores y el microservicio administrador.

## 1. Topología del clúster

| Componente | Cantidad | Rol |
|------------|----------|-----|
| Zookeeper | 3 nodos | Coordinación (elección de líder, metadatos). Quorum de 3 → tolera la caída de 1 nodo. |
| Broker Kafka | 3 brokers | Almacenamiento y réplica de particiones. |
| Kafka UI | 1 | Administración visual (brokers, tópicos, particiones, offsets, lag, consumidores). |

Los 3 brokers usan `replication.factor=3` y `min.insync.replicas=2`. Esto significa que
cada partición tiene 1 líder + 2 réplicas seguidoras distribuidas entre brokers, y que una
escritura con `acks=all` solo se confirma cuando al menos 2 réplicas ISR la han recibido.

**Qué se tolera:**
- Caída de 1 broker → el líder de una partición es re-elegido entre las réplicas ISR sin pérdida.
- Caída de 1 Zookeeper → el quorum (2/3) sigue operando.

## 2. Tópicos (3 particiones + 3 réplicas)

| Tópico | Particiones | Réplicas | retention.ms | cleanup.policy | Motivo |
|--------|-------------|----------|--------------|----------------|--------|
| `pedidos360.pedidos.eventos` | 3 | 3 | 7 días (604800000) | `delete` | Flujo de eventos de dominio: retención acotada para no crecer indefinidamente. |
| `pedidos360.analitica.metricas` | 3 | 3 | 30 días (2592000000) | `compact,delete` | Métricas por clave (`estado`): se conserva el último valor por clave. |

La creación es **idempotente y doble**:
- Script `infra/apps/kafka/create-topics.sh` (contenedor one-shot `kafka-init`).
- Beans `NewTopic` en `analytics-service` (`KafkaTopicsConfig`) vía `KafkaAdmin`.

## 3. Paralelismo

- **3 particiones** permiten hasta **3 consumidores concurrentes** dentro de un consumer group,
  cada uno leyendo de una partición distinta (`factory.setConcurrency(3)` en `KafkaConsumerConfig`).
- El **particionado por clave** garantiza orden: `pedidos-service` publica con `key = número de
  pedido` (eventos de un pedido en orden); `analytics-service` publica métricas con `key = estado`
  (agregación y compactación coherentes por estado).
- Los mensajes **sin clave** usan el *sticky partitioner* (batches por eficiencia), sin garantía
  de partición ni de orden.

## 4. Procesamiento distribuido (consumer groups)

| Consumer group | Microservicio | Tópico que consume | Responsabilidad |
|----------------|---------------|--------------------|-----------------|
| `pedidos360-analytics-group` | analytics-service | `pedidos360.pedidos.eventos` | Agregar métricas por estado en PostgreSQL y publicar resultados (`auto-offset-reset=earliest`: no perder historia). |
| `pedidos360-alertas-group` | consumer-service | `pedidos360.pedidos.eventos` | Alertas en tiempo real: alto valor, cancelación, entrega (`latest`: solo eventos nuevos). |
| `pedidos360-reporting-group` | analytics-service | `pedidos360.analitica.metricas` | Vista materializada del tópico compactado (último valor por estado). |
| `pedidos360-dlt-monitor-group` | analytics-service | `pedidos360.pedidos.eventos.dlt` | Registrar con contexto los mensajes que fallaron. |

**Compartir vs. dividir:** `analytics-group` y `alertas-group` son **dos microservicios distintos
sobre el mismo tópico**: cada grupo recibe *todos* los eventos (publish/subscribe, se comparten los
datos). Dentro de cada grupo, los 3 hilos (`concurrency=3`) se **dividen** las 3 particiones
(balanceo); si se escala a más réplicas del servicio, Kafka rebalancea automáticamente.

Cada grupo mantiene **sus propios offsets**, por lo que el segundo grupo puede leer los mismos
mensajes sin interferir con el primero (distribución de responsabilidades). Kafka reparte las
particiones de un tópico entre los miembros del grupo (balanceo de carga); al caer un miembro,
las particiones se reasignan (resiliencia).

## 5. Productores (cliente)

`pedidos-service` (`KafkaProducerConfig` + `PedidoEventKafkaProducer`):
- Serializador `StringSerializer` (clave) + `JsonSerializer` (valor, **sin** encabezados de tipo).
- `acks=all`, `enable.idempotence=true`, `retries=3` → entrega fiable y sin duplicados.

`analytics-service` (`MetricasProducer`): publica `MetricasPedidoDto` con `key = estado`.

## 6. Consumidores y resiliencia

`analytics-service` (`PedidoEventConsumer`, `MetricasConsumer`):
- `@KafkaListener` con `@Payload` tipado (DTOs bien estructurados) y headers de partición/offset.
- Commit controlado por el contenedor (`ENABLE_AUTO_COMMIT_CONFIG=false` + `AckMode.RECORD`):
  el offset se confirma solo tras procesar (o desviar a DLT) cada registro.
- `ErrorHandlingDeserializer` envolviendo al `JsonDeserializer`: un payload que no es JSON válido
  (*poison pill*) no rompe el `poll()` ni bloquea la partición; llega como
  `DeserializationException` y se envía **directo** a la DLT con sus bytes originales.
- `DefaultErrorHandler` con `DeadLetterPublishingRecoverer`:
  1. Validación del payload (pedidoId, estado, total). Si es un *poison message*
     (`NonRetryableEventException`) → va directo a la DLT sin reintentos.
  2. Errores transitorios → **3 reintentos** con `FixedBackOff(1000 ms)`.
  3. Agotados los reintentos → el mensaje se publica en `pedidos360.pedidos.eventos.dlt`
     (preservando la partición original).
- Logging contextual en cada recepción, reintento, fallo y desvío a DLT.
- `DeadLetterConsumer` lee la DLT y registra tópico/partición/offset de origen, excepción y payload.
- **Una sola DLT para ambos consumidores** (eventos y métricas), por diseño: mantiene exactamente 2
  tópicos de datos + 1 auxiliar. El origen no se pierde: `DeadLetterPublishingRecoverer` agrega los
  encabezados `kafka_dlt-original-topic/-partition/-offset` y el monitor los registra
  (`origen=<tópico>-<partición>@<offset>`). Ambos tópicos tienen 3 particiones, así que la partición
  original siempre existe en la DLT.

`consumer-service` (`PedidoAlertasConsumer`): mismo esquema (`ErrorHandlingDeserializer`, 2
reintentos, eventos inválidos → `NonRetryableException` → se registran y se saltan).

## 7. Microservicio administrador (`kafka-admin-service`)

Usa `AdminClient` para la gestión programática:

- Tópicos: `POST /api/v1/kafka/topics`, `GET .../topics`, `GET .../topics/{name}`,
  `PUT .../topics/{name}/configs` (alterar retention/cleanup/min.insync.replicas),
  `POST .../topics/{name}/partitions` (aumentar particiones para escalar consumidores),
  `DELETE .../topics/{name}`.
- Consumer groups: `GET .../consumer-groups`, `GET .../consumer-groups/{groupId}`,
  `GET .../consumer-groups/{groupId}/offsets` (offset confirmado, log-end offset y **lag** por
  partición), `POST .../consumer-groups/{groupId}/offsets/reset` (earliest/latest, para reprocesar
  o saltar backlog; el grupo debe estar inactivo), `DELETE .../consumer-groups/{groupId}`.
- Clúster: `GET .../cluster` (id, controlador y brokers activos).

**Lectura del lag:** lag ≈ 0 → el grupo va al día; lag creciendo de forma sostenida → consumidor
caído, lento o reintentando (anticipa un problema antes de que el usuario lo note); lag concentrado
en una sola partición → clave "caliente" (mala distribución de claves).

Los DTOs están validados con Jakarta (`@NotBlank`, `@Pattern`, `@Min`) → HTTP 400 ante
configuraciones inválidas, y `GlobalExceptionHandler` centraliza los errores sin filtrar
stack traces.
