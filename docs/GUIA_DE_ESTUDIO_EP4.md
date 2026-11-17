# Guía de Estudio — Pedidos360 (EP4): Entender y defender el proyecto

> Objetivo: que puedas **explicar con tus palabras** qué hace cada pieza del sistema y
> responder cualquier pregunta del evaluador. Todo está escrito pensando en alguien que
> recién empieza con el tema: primero la idea simple, luego el detalle técnico.

---

## 0. La idea en una sola frase

> **Pedidos360** es una aplicación de gestión de pedidos construida como un conjunto de
> **microservicios** (piezas pequeñas e independientes) que se comunican entre sí de dos
> formas: por **HTTP** (petición/respuesta) y por **mensajería asíncrona** (RabbitMQ).

Cuando un usuario crea un pedido, ocurre una cadena:

```
Navegador (Angular)
   │  login con usuario/contraseña → Keycloak devuelve un JWT (un "carnet de identidad" digital)
   ▼
Frontend llama a la API  ──►  API Gateway  ──►  BFF  ──►  pedidos-service
   (adjuntando el JWT)      (valida el JWT)    (orquesta)   (guarda en PostgreSQL)
                                                                  │
                                                          publica un "evento"
                                                                  ▼
                                                     RabbitMQ (colas/exchanges)
                                                                  ▼
                                             consumer-service procesa (ACK o DLQ)
```

---

## 1. Conceptos base que debes poder definir

### 1.1 ¿Qué es un microservicio?
Una aplicación partida en **servicios pequeños, independientes y especializados**, cada
uno con su propia responsabilidad. En vez de un "mega-programa" (monolito), tienes piezas
que se pueden desplegar, escalar y modificar por separado.

En tu proyecto son 6 microservicios (más el frontend y los servicios de infraestructura):

| Pieza | Qué hace |
|---|---|
| `pedidos360-frontend` | Interfaz web (Angular). Lo que ve el usuario. |
| `pedidos360-gateway` | **API Gateway**: puerta única de entrada + valida el JWT. |
| `pedidos360-bff` | **BFF**: orquesta y enruta hacia pedidos/productos. |
| `pedidos360-pedidos-service` | CRUD de pedidos + **productor** de eventos RabbitMQ. |
| `pedidos360-productos-service` | CRUD de productos. |
| `pedidos360-consumer-service` | **Consumidores** RabbitMQ (notificaciones, tickets, documentos). |
| `pedidos360-admin-service` | **Administrador** de RabbitMQ por REST. |
| Keycloak | **IDaaS** (login y emisor de JWT). |
| RabbitMQ (2 nodos) | Broker de mensajería. |
| PostgreSQL | Base de datos relacional. |

### 1.2 ¿Qué es Docker y docker-compose?
- **Docker** empaqueta cada servicio en un "contenedor": un entorno aislado que incluye
  todo lo necesario para correr (código + dependencias). "En mi máquina funciona" deja de
  ser un problema.
- **docker-compose** es un archivo (`infra/apps/docker-compose.yml`) que describe **todos
  los contenedores a la vez** y cómo se conectan. Con `docker compose up` levantas el
  sistema completo.

### 1.3 ¿Qué es "cloud native"?
Una forma de construir software pensada para **la nube**: contenedores, microservicios,
configuración por variables de entorno (no secretos en el código), escalabilidad y
resiliencia (si una pieza falla, el resto sigue). Tu proyecto aplica estos principios.

---

## 2. Seguridad: login, JWT, OAuth/OIDC e IDaaS

### 2.1 La idea simple
En lugar de que cada servicio pida usuario/contraseña, usas un **proveedor de identidad
(IDaaS)** que se encarga de la autenticación y entrega un **JWT**. Ese JWT viaja en cada
petición y los servicios lo **validan** (sin volver a preguntar la contraseña).

### 2.2 ¿Qué es un JWT (JSON Web Token)?
Un texto con **3 partes separadas por puntos**: `header.payload.signature`.

- **header:** dice qué algoritmo se usó (ej. RS256).
- **payload:** los *claims* (datos): quién es el usuario (`sub`), qué roles tiene, el
  `iss` (emisor), el `aud` (audiencia), y el `exp` (vencimiento).
- **signature:** firma que garantiza que el token **no fue manipulado**. Si alguien cambia
  una letra, la firma deja de coincidir y se rechaza.

**Analogía:** el JWT es un carnet con foto y firma. El guardia (Gateway/BFF) no necesita
conocer al dueño: confía en la firma de quien lo emitió (Keycloak).

### 2.3 OAuth 2.0 y OpenID Connect (OIDC)
- **OAuth 2.0** es un estándar de *autorización* (qué puede hacer alguien).
- **OpenID Connect (OIDC)** es una capa sobre OAuth que añade *autenticación* (quién es
  alguien). Devuelve un `id_token` además del `access_token`.
- Tu frontend usa el flujo **Authorization Code + PKCE**, el recomendado para SPA:
  1. El usuario pulsa "Iniciar sesión".
  2. Se redirige a Keycloak.
  3. Se autentica y Keycloak devuelve un *code*.
  4. Ese code se intercambia por el JWT.
  5. PKCE evita que un atacante robe el code.

### 2.4 IDaaS y "tenant"
- **IDaaS** = *Identity as a Service*: identidad como servicio (Azure AD/Entra ID, Okta,
  Auth0, Keycloak).
- **Tenant** (Azure) o **realm** (Keycloak) = tu "espacio aislado" de usuarios y clientes.
  Tu proyecto define el realm `pedidos360` con el cliente `pedidos360-spa` y 3 usuarios.

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `operador` | `operador123` | OPERADOR |
| `cliente` | `cliente123` | CLIENTE |

### 2.5 ¿Cómo valida el Gateway el JWT? (lo más preguntado)
El Gateway (`SecurityConfig` reactiva + `JwtDecoderConfig`) usa Spring Security como
*resource server*. Antes de enrutar:
1. Lee el token del header `Authorization: Bearer <token>`.
2. Descarga las claves públicas de Keycloak (JWKS) y **verifica la firma**.
3. Valida el `issuer` (debe ser `http://localhost:8090/realms/pedidos360`).
4. Valida el `audience` (debe ser `pedidos360-spa`).
5. Valida el `exp` (no vencido).

Resultado: **token válido → 200/reenvía**; **sin token / manipulado / expirado → 401**.
Esto demuestra que el rechazo ocurre **en el Gateway, antes de llegar a los microservicios**.

### 2.6 ¿Qué es el BFF y para qué sirve?
*Backend-For-Frontend*: un servicio intermedio hecho a medida del frontend. Evita que la
SPA hable directo con 10 microservicios; el BFF **agrupa y orquesta** las llamadas y aplica
la **autorización fina por rol** (`@PreAuthorize`). Ejemplos en tu código:
- `@PreAuthorize("isAuthenticated()")` → cualquier usuario logueado puede listar pedidos.
- `@PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")` → solo admin/operador cambian estado.

---

## 3. Mensajería con RabbitMQ (corazón del EP)

### 3.1 ¿Por qué mensajería y no solo HTTP?
HTTP es **síncrono** (pregunto y espero respuesta). La mensajería es **asíncrona** y
**desacoplada**: el productor publica un mensaje y sigue trabajando; no le importa cuándo
ni quién lo procese. Ventajas: tolerancia a fallos, escalabilidad y desacoplamiento.

**Ejemplo:** al crear un pedido no quieres bloquear al usuario esperando que se envíe el
email o se genere un documento. Guardas el pedido, publicas un "evento" y listo; otro
servicio lo procesa en segundo plano.

### 3.2 Los 5 conceptos clave de RabbitMQ
| Concepto | Qué es | Analogía |
|---|---|---|
| **Productor** | Quien publica mensajes (`pedidos-service`). | Quien echa una carta al buzón. |
| **Consumidor** | Quien los recibe y procesa (`consumer-service`). | Quien abre el buzón. |
| **Cola (Queue)** | Fila donde esperan los mensajes (FIFO). | El buzón / la fila de espera. |
| **Exchange** | Central que decide a qué cola(es) va cada mensaje. | Central de correos. |
| **Binding + routing key** | La "regla" que une exchange y cola. | La dirección/etiqueta de la carta. |

### 3.3 Tipos de exchange (pregunta fija)
- **Direct:** enruta por **coincidencia exacta** de la routing key. `pedido.creado` → solo
  la cola con binding `pedido.creado`.
- **Topic:** enruta por **patrones** con comodines. `*` = una palabra, `#` = cero o más.
  `pedido.#` recibe **todo** lo que empiece por `pedido.`.
- (Otros: fanout = a todas las colas; headers = por cabeceras.)

En tu proyecto:
- `pedidos360.topic` (topic) con `pedido.#` → notificaciones y `pedido.estado.entregado` → documentos.
- `pedidos360.direct` (direct) con `pedido.creado` → tickets.

### 3.4 Tu topología (memorízala)
| Exchange | Tipo | Routing key | Cola destino |
|---|---|---|---|
| `pedidos360.topic` | topic | `pedido.#` | `pedidos360.notificaciones.queue` |
| `pedidos360.topic` | topic | `pedido.estado.entregado` | `pedidos360.documentos.queue` |
| `pedidos360.direct` | direct | `pedido.creado` | `pedidos360.tickets.queue` |
| `pedidos360.dlx` | direct (DLX) | (nombre de la DLQ) | las 3 DLQ |

Cada cola de negocio tiene su DLQ: `...notificaciones.dlq`, `...tickets.dlq`,
`...documentos.dlq`.

### 3.5 ¿Qué es una DLQ y un DLX? (pregunta fija)
- **DLX (Dead Letter Exchange):** un exchange especial al que RabbitMQ **reenvía** un
  mensaje cuando éste no puede procesarse.
- **DLQ (Dead Letter Queue):** la cola "cementerio" que recibe esos mensajes.

Configuras en la cola principal dos argumentos: `x-dead-letter-exchange` (a qué DLX mandar)
y `x-dead-letter-routing-key`. Un mensaje va a la DLQ cuando: **(a)** es rechazado con
`requeue=false`, **(b)** expira (TTL) o **(c)** la cola se llena.

**Para qué sirve:** no perder mensajes con error y evitar el **bucle infinito** (que un
mensaje malo se reintente para siempre bloqueando la cola).

### 3.6 ACK / NACK / requeue (pregunta fija)
Cuando un consumidor recibe un mensaje, debe **confirmar** qué pasó:

| Acción | Significado | Resultado |
|---|---|---|
| `basicAck(tag, false)` | "Lo procesé bien" | El mensaje se elimina de la cola. |
| `basicNack(tag, false, false)` | "Falló y no lo reintentes" (`requeue=false`) | Va a la **DLQ**. |
| `basicNack(tag, false, true)` | "Falló, devuélvelo" (`requeue=true`) | Vuelve a la cola (**reintento**). |

Tu proyecto usa **ACK manual** (`acknowledge-mode: manual`), lo que te da control total
sobre estas decisiones en el código.

### 3.7 Error transitorio vs irrecuperable (lo que acabas de arreglar)
- **Transitorio:** el mensaje es válido, falló por algo temporal (red, DB caída) →
  **reintento**.
- **Irrecuperable ("poison message"):** nunca podrá procesarse (payload vacío, formato
  malo) → **DLQ**.

En el código lo distinguimos con `NonRetryableException`: si es de ese tipo → DLQ; si es
cualquier otra excepción → reintento.

### 3.8 Clúster de 2 nodos
Un **clúster** son varios brokers RabbitMQ que trabajan como uno solo (comparten colas y
exchanges), lo que da **alta disponibilidad**: si un nodo cae, el otro sigue.

Para que dos nodos formen clúster necesitan la **misma cookie Erlang**
(`RABBITMQ_ERLANG_COOKIE`) — es como una "contraseña" que les permite confiar entre sí.
Tu `rabbitmq.conf` usa *peer discovery* clásico listando `rabbit@rabbitmq1` y
`rabbit@rabbitmq2`, y el healthcheck del nodo 2 verifica que realmente esté en el clúster.

---

## 4. Preguntas probables y respuestas modelo

**1. "¿Qué es RabbitMQ y por qué lo usaste?"**
Es un *message broker* (intermediario de mensajes) que permite comunicación **asíncrona y
desacoplada** entre servicios. Lo uso para que, al crear un pedido, el sistema no se bloquee
esperando tareas secundarias (enviar notificación, emitir ticket, generar documento): el
servicio publica un evento y sigue; los consumidores lo procesan cuando pueden.

**2. "¿Diferencia entre exchange direct y topic?"**
En *direct* la routing key debe coincidir **exactamente**; en *topic* se usan **patrones**
con `*` (una palabra) y `#` (cero o más palabras). Ej.: `pedido.#` recibe todo lo que
empieza con `pedido.`.

**3. "¿Qué es una DLQ y cómo se configura?"**
Es la cola a donde van los mensajes que no pudieron procesarse. Se configura en la cola
principal con `x-dead-letter-exchange` y `x-dead-letter-routing-key`. Sirve para no perder
mensajes con error y evitar reintentos infinitos.

**4. "¿Qué pasa si un consumidor falla al procesar?"**
Con ACK manual, el consumidor decide: si el error es **irrecuperable** hace
`basicNack(requeue=false)` y el mensaje va a la **DLQ**; si es **transitorio** hace
`basicNack(requeue=true)` y se **reintenta**; si procesa bien, hace `basicAck`.

**5. "¿Cómo se forma el clúster de 2 nodos?"**
Ambos nodos comparten la misma `RABBITMQ_ERLANG_COOKIE` y usan *peer discovery* clásico
(`rabbitmq.conf` lista los dos nodos), por lo que `rabbitmq2` se une a `rabbitmq1`
automáticamente al arrancar.

**6. "¿Cómo valida el Gateway el JWT?"**
Actúa como *OAuth2 resource server*: obtiene las claves públicas de Keycloak (JWKS),
verifica la **firma**, el **issuer**, el **audience** y el **vencimiento**. Válido → 200;
inválido/ausente/expirado → 401.

**7. "¿Qué es el BFF y por qué existe?"**
Es un *Backend-For-Frontend*: un servicio intermedio que agrupa llamadas para el frontend
y aplica autorización fina por rol (`@PreAuthorize`), evitando que la SPA hable directo con
todos los microservicios.

**8. "¿Cómo se autentica el frontend?"**
Con **OpenID Connect (Authorization Code + PKCE)** contra Keycloak (IDaaS). Obtiene un JWT,
que un *interceptor* adjunta (`Authorization: Bearer ...`) en cada petición al Gateway.

**9. "¿Qué roles hay y cómo se autoriza?"**
`ADMIN`, `OPERADOR` y `CLIENTE`. El BFF mapea los roles del JWT y usa `@PreAuthorize`
(p. ej. solo ADMIN/OPERADOR cambian estado de pedidos). En el frontend un guard y la navbar
ocultan opciones según el rol.

**10. "¿Qué hace el admin-service?"**
Expone endpoints REST (`POST /api/v1/admin/queues`, `DELETE /queues/{name}`, exchanges,
bindings) para administrar la topología de RabbitMQ. Toda la lógica está en
`RabbitAdminService` (usa `AmqpAdmin`) y los DTOs validan la entrada (`@NotBlank`, `@Pattern`).

**11. "¿Por qué centralizas los nombres de colas/exchanges?"**
Para tener **un solo punto de verdad** y no repetir literales ("hardcoding") en el negocio.
Así, si cambio un nombre, lo hago en un solo lugar (`application.yml`) sin tocar la lógica.
Además, separa la **configuración de infraestructura** de la **lógica de negocio**.

**12. "Muéstrame el flujo completo."**
Frontend → (JWT) → Gateway (valida) → BFF (autoriza) → pedidos-service (guarda en
PostgreSQL y publica `pedido.creado`) → RabbitMQ enruta → consumidores de notificaciones y
tickets procesan (ACK). Si un mensaje falla irrecuperablemente, va a su DLQ.

---

## 5. Cómo demostrarlo en vivo (checklist rápido)

1. `docker compose up -d --build` → `docker compose ps` (todo `healthy`).
2. Clúster: `docker compose exec rabbitmq1 rabbitmqctl cluster_status` → 2 nodos.
3. Management UI `http://localhost:15672` → ver 3 colas + 3 DLQ + exchanges direct/topic.
4. Token: `POST /realms/pedidos360/protocol/openid-connect/token` (password grant).
5. Gateway con token → 200; sin token → 401.
6. Crear pedido → ver logs del `consumer-service` (ACK).
7. Publicar mensaje vacío → ver `NACK` → mensaje en la DLQ.

> Los comandos exactos están en `docs/GUION_DEMO_EP4.md`.

---

## 6. Glosario rápido

| Término | Significado |
|---|---|
| JWT | Token firmado con datos (claims) de identidad. |
| OIDC | OpenID Connect: autenticación sobre OAuth 2.0. |
| IDaaS | Identidad como servicio (Keycloak/Azure AD). |
| Realm/Tenant | Espacio aislado de usuarios y clientes. |
| BFF | Backend-For-Frontend. |
| Exchange | Central que enruta mensajes a colas. |
| Routing key | Clave que decide el enrutado. |
| Binding | Unión exchange ↔ cola con una routing key. |
| DLX/DLQ | Dead Letter Exchange / Queue (mensajes fallidos). |
| ACK/NACK | Confirmación positiva/negativa de un mensaje. |
| Cluster | Varios brokers actuando como uno. |
| Docker | Contenedores aislados y portables. |
