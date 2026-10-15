# Bitácora — Pedidos360 (EP3 + EP4) — Arquitectura Local

**Fecha:** 15 de octubre de 2026
**Proyecto:** Pedidos360 (EP3 + EP4) — Cloud Native

---

## 1. Resumen del trabajo realizado

### 1.1 Frontend Angular 18 (SPA standalone)

- Login OIDC con **Keycloak** (Authorization Code + PKCE) vía `keycloak-angular` v16.
- Vistas Dashboard / Pedidos / Productos, diseño responsivo (navbar hamburguesa, modal, formularios).
- Consume el **API Gateway** (`http://localhost:8080`) como punto único de entrada.

### 1.2 Backend — microservicios Spring Boot 3.3 (Java 21)

- `pedidos360-gateway` — **API Gateway (Spring Cloud Gateway)**. Valida el JWT de Keycloak
  (issuer + audience + firma) antes de enrutar al BFF.
- `pedidos360-bff` — Backend-For-Frontend. Valida JWT de Keycloak (defensa en profundidad),
  aplica `@PreAuthorize` por rol y enruta a pedidos/productos.
- `pedidos360-pedidos-service` — CRUD de pedidos + **productor** de eventos RabbitMQ
  (publisher confirms, `correlationId`, exchanges declarados).
- `pedidos360-productos-service` — CRUD de productos.
- `pedidos360-consumer-service` — consumidores por dominio (`notifications`, `tickets`,
  `documents`) con **ACK manual** (`basicAck`/`basicNack` → DLQ) y
  `SimpleRabbitListenerContainerFactory` con `defaultRequeueRejected(false)`.
- `pedidos360-admin-service` — administrador de RabbitMQ (REST: crear/eliminar colas,
  exchanges, bindings + DTOs validados con `@NotBlank`/`@Pattern`).

### 1.3 Mensajería RabbitMQ (EP3)

- Config **100% centralizada** (cero hardcoding): nombres de colas/exchanges/DLX/routing
  keys en `application.yml` + `@ConfigurationProperties`.
- Beans explícitos de `Queue`, `TopicExchange`, `DirectExchange` y `Binding`.
- Topología: **3 colas de negocio + 3 DLQ** + exchanges `direct`/`topic`/`dlx`.

### 1.4 Infraestructura local (EP4)

- `docker-compose.yml` con: **clúster RabbitMQ de 2 nodos** (join automático por *peer
  discovery* clásico + healthcheck `cluster_status`), **Keycloak** (IDaaS, realm
  preconfigurado), **Spring Cloud Gateway**, **PostgreSQL**, 5 microservicios + frontend.
- Todo corre **100% local** (sin AWS/Azure/GCP).

> **Nota histórica:** el despliegue anterior sobre **AWS (EC2 + API Gateway) + Azure AD**
> quedó **obsoleto**. La arquitectura vigente es 100% local con Docker Compose.

---

## 2. Arquitectura (vigente)

| Componente | Tecnología | Puerto (host) |
|-----------|-----------|---------------|
| **Gateway** | Spring Cloud Gateway (valida JWT) | `8080` |
| **Frontend** | Angular 18 + keycloak-angular (nginx) | `4200` |
| **Keycloak** | IDaaS local (realm `pedidos360`) | `8090` |
| BFF | Spring Boot 3.3 | `8081` |
| pedidos-service | Spring Boot 3.3 (+ productor RabbitMQ) | `8082` |
| productos-service | Spring Boot 3.3 | `8083` |
| consumer-service | Spring Boot 3.3 (consumidores RabbitMQ) | `8084` |
| admin-service | Spring Boot 3.3 (admin RabbitMQ) | `8085` |
| RabbitMQ | clúster 2 nodos (`rabbitmq1` + `rabbitmq2`) | `5672` / `15672` |
| PostgreSQL | 16 | `5433` |

**Flujo de una petición autenticada:**

```
Navegador → Frontend(:4200) → Gateway(:8080, valida JWT) → BFF(:8081, valida JWT + roles)
  → pedidos-service(:8082) / productos-service(:8083)
```

**Flujo de mensajería (asíncrono):**

```
pedidos-service (productor) → RabbitMQ (topic/direct) → consumer-service
  → basicAck (éxito) | basicNack requeue=false → DLX → DLQ
```

---

## 3. Cómo levantar el entorno (resumen rápido)

Ver **`docs/GUION_DEMO_EP4.md`** (guion completo para el evaluador) y
**`docs/RUNBOOK_DESPLIEGUE.md`**.

```bash
cd infra/apps
cp .env.example .env        # opcional
docker compose up -d --build
docker compose ps           # todos "healthy"
```

---

## 4. Git

- `feature/rabbitmq-mensajeria` → `feature/rabbitmq-resiliencia` → merge en `develop`.
- Reestructuración (Keycloak + Gateway + contenedorización frontend) aplicada sobre `develop`.
- Modelo de ramas: `feature/* → develop → main`.

---

## 5. Credenciales de prueba (Keycloak)

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `operador` | `operador123` | `OPERADOR` |
| `cliente` | `cliente123` | `CLIENTE` |

RabbitMQ Management: `http://localhost:15672` (`guest` / `guest`).
Keycloak Admin Console: `http://localhost:8090` (`admin` / `admin`).
