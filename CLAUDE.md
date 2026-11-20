# Pedidos360 — Contexto del proyecto (leer primero)

Proyecto académico **DSY1107 (Duoc UC)**: arquitectura Cloud Native de gestión de pedidos.
Este archivo resume el estado actual para retomar trabajo sin re-investigar.
Detalle extendido: `docs/ESTADO_Y_CONTEXTO.md`.

## Evaluaciones

| Evaluación | Qué pide | Estado |
|------------|----------|--------|
| **EP5** (encargo, código) | Kafka: clúster 3 ZK + 3 brokers, 2 tópicos 3×3, productor/consumidores, consumer groups, DLT, admin programático, analítica en frontend | Completo — **v1.2.0** en `main` (tags `v1.0.0`, `v1.1.0`, `v1.2.0`) |
| **EP6** (presentación) | Exposición oral sobre Kafka + demo en la nube: API Manager, IDaaS (tenant + usuarios), OAuth2/OIDC + JWT, API Manager valida JWT, backend/frontend desplegados, RabbitMQ 2 nodos, 3 colas + DLQ, exchanges direct/topic, docker-compose en la nube, Kafka UI 2 tópicos 3×3 | Desplegado y validado en AWS |

## Arquitectura en 1 minuto

- **Frontend** Angular 18 (`pedidos360-frontend`), login OIDC con `angular-oauth2-oidc` (Authorization Code + PKCE).
  Config OIDC en **tiempo de ejecución**: nginx genera `/config.json` desde env vars
  (`docker/40-runtime-config.sh`); sin `AUTH_ISSUER` usa `environment.ts` (Keycloak local).
- **Backend** Spring Boot 3.3 / Java 21: `gateway` (Spring Cloud Gateway, valida JWT) → `bff`
  (re-valida JWT + roles) → `pedidos-service`, `productos-service`, `analytics-service`.
  Además `consumer-service` (RabbitMQ + alertas Kafka), `admin-service` (admin RabbitMQ),
  `kafka-admin-service` (AdminClient).
- **Mensajería**: RabbitMQ (transaccional, 3 colas + 3 DLQ, exchanges topic/direct/dlx) y
  Kafka (streaming: `pedidos360.pedidos.eventos`, `pedidos360.analitica.metricas` compactado,
  DLT `pedidos360.pedidos.eventos.dlt`).
- **Eventos de pedido** (`PedidoEvent`): `eventId`, `tipoEvento` (`PEDIDO_CREADO`, `ESTADO_ACTUALIZADO`,
  `PEDIDO_ELIMINADO`), `ocurridoEn` + datos del pedido. Se publican **después del commit** (RabbitMQ y Kafka).
- **Analítica idempotente**: `analytics-service` guarda el último estado de cada pedido (`pedido_estado_actual`);
  un cambio de estado hace -1/+1 en `metrica_pedido`, una eliminación descuenta y un `eventId` repetido se ignora.
  Para recalcular: detener analytics, `TRUNCATE metrica_pedido, pedido_estado_actual`, resetear
  `pedidos360-analytics-group` a `--to-earliest` y volver a arrancar (replay de Kafka).
- **RabbitMQ**: clientes conectan a ambos nodos (`RABBITMQ_ADDRESSES`); el productor usa publisher confirms +
  returns; consumidores con ACK manual: irrecuperable → DLQ, transitorio → 1 reintento y luego DLQ (`RechazoMensaje`).
- **Identidad**: local = Keycloak; nube = **Amazon Cognito**. El backend acepta ambos:
  audiencia por `aud` (Keycloak) o `client_id` (Cognito); roles desde `realm_access.roles` o `cognito:groups`.

## Despliegue en AWS (Learner Lab)

- **Un solo comando, idempotente**: `bash infra/aws/deploy.sh` (Git Bash, desde la raíz).
  - Sesión nueva misma cuenta: `SKIP_CODE=1 bash infra/aws/deploy.sh` (enciende EC2, ajusta IP, verifica).
  - Otra cuenta/taller: mismo comando, crea todo (SG, 2 EC2 + EIP, Cognito, API Gateway, código, `.env`).
  - Borrar todo: `bash infra/aws/destroy.sh`.
- Prerrequisitos: credenciales del lab en `~/.aws/credentials` (expiran ~4 h) y llave en `~/.ssh/labsuser.pem`.
- Recursos se identifican **por nombre/tag** con prefijo `pedidos360` (nunca por ID fijo):
  `pedidos360-sg`, EC2 `pedidos360-messaging` / `pedidos360-apps`, EIPs con el mismo tag,
  API `pedidos360-api-manager`, User Pool `pedidos360-tenant`, client `pedidos360-spa`, authorizer `cognito-jwt`.
- Compose en la nube: `infra/aws/docker-compose.messaging.yml` (Kafka + RabbitMQ + Kafka UI) y
  `infra/aws/docker-compose.apps.yml` (Postgres + servicios + frontend). El `.env` lo genera
  `deploy.sh` en cada host (`~/pedidos360/infra/aws/.env`, gitignored) y **conserva** las claves existentes.
- API Gateway inyecta `X-Origin-Verify` (secreto `ORIGIN_SECRET` en el `.env`, guardado también en la integración);
  sin ese encabezado el gateway responde 403 en `/api/**`. Para probar en la EC2 sin pasar por API Gateway, enviar el encabezado.
- Usuarios demo Cognito: `admin` (ADMIN), `operador` (OPERADOR), `cliente` (CLIENTE); claves y matriz de permisos en
  `docs/CREDENCIALES_DEMO.md` (las claves aleatorias de BD/RabbitMQ no se versionan: están en el `.env` de cada EC2).
- Última cuenta usada: 381492248228 (us-east-1), API `https://136xrpjdoj.execute-api.us-east-1.amazonaws.com`,
  messaging EIP 44.213.34.232 (Kafka UI :8091, RabbitMQ :15672/:15673), apps EIP 52.45.200.182.
  En otra cuenta estos valores cambian: los imprime `deploy.sh` al final.

## Restricciones y trampas conocidas

- Learner Lab: máximo `t3.large` (por eso 2 hosts); credenciales temporales; EC2 se detienen al cerrar el lab
  (los contenedores tienen `restart: unless-stopped` y vuelven solos).
- Git Bash en Windows: usar `MSYS_NO_PATHCONV=1` con rutas tipo `/dev/xvda`; el home tiene espacio
  (`C:\Users\Victor Barrera`) → siempre `"$HOME/.ssh/labsuser.pem"` entre comillas; `aws.exe` necesita rutas `cygpath -m`.
- Scripts `.sh` deben ir en LF (`.gitattributes`); en los hosts `deploy.sh` normaliza CRLF.
- Zookeeper necesita `-Dzookeeper.4lw.commands.whitelist=ruok,...` para su healthcheck.
- Java 21 y Maven sí están en el PC (`mvn clean verify` por servicio); Docker **no**: `docker compose` solo en las EC2.
- Cambiar 1 carácter final de una firma JWT puede no invalidarla (bits de relleno base64): para probar
  rechazo, alterar el payload.

## Pendientes

- Hecho: v1.0.0 (PR #17 → `develop`, PR #18 → `main`, tag y release `v1.0.0`). Detalle en `CHANGELOG.md`.
- Hecho: v1.1.0 = P1 de mensajería (PR #19). Limitación: colas RabbitMQ *classic* en `rabbitmq1` (HA real
  requiere colas quorum con ≥3 nodos).
- Hecho: v1.2.0 = P2 (PR #22): secreto `X-Origin-Verify` entre API Gateway y gateway (directo a :8080
  → 403); número de pedido = `PED-%06d` del id; DLT compartida documentada; lag N/A era un grupo de consola.
- Hecho: login interactivo probado en navegador (Cognito Hosted UI, Authorization Code + PKCE → dashboard; todo `/api` vía API Gateway).
- `/api/analitica/reporting`: vista en memoria que se reconstruye al arrancar releyendo el tópico compactado
  (consistencia eventual: unos segundos tras un reinicio).
- Hecho: gateway y BFF rechazan ID tokens (`AccessTokenValidator`: `token_use=id` / `typ=ID` → 401).

## Decisiones

- **Base de datos**: PostgreSQL en contenedor dentro de la EC2 de apps (no RDS), por decisión del equipo.
- Re-auditoría EP5/EP6 completa (2026-11-19): todos los bloques en orden; DB en EC2 aceptada.

## Convenciones

- Ramas `feature/*`/`fix/* → develop → main` (vía PR con `gh`); commits en español estilo conventional;
  releases con tag `vX.Y.Z` + `CHANGELOG.md`; desplegar desde `git archive HEAD` (no sube archivos sin trackear).
- Nunca commitear `.env` ni credenciales.
- Documentación: `README.md` (visión general), `docs/DESPLIEGUE_AWS.md` (nube),
  `docs/ARQUITECTURA_KAFKA.md`, `docs/GUIA_DE_ESTUDIO_EP6.md` (estudio para la presentación).
