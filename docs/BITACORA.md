# Bitácora y Runbook de Despliegue — Pedidos360

**Fecha:** 13 de octubre de 2026
**Proyecto:** Pedidos360 (EP3 + EP4) — Cloud Native

---

## 1. Bitácora del proyecto

### Trabajo realizado (resumen cronológico)

1. **Frontend Angular 18** (SPA standalone): login OIDC con MSAL (Authorization Code + PKCE), vistas Dashboard/Pedidos/Productos, diseño responsivo (navbar hamburguesa, modal, formularios).
2. **Backend microservicios Spring Boot 3.3 (Java 21):**
   - `pedidos360-bff` — Backend-For-Frontend: valida JWT (Azure AD) y enruta a los servicios.
   - `pedidos360-pedidos-service` — CRUD de pedidos + **productor** de eventos RabbitMQ.
   - `pedidos360-productos-service` — CRUD de productos.
3. **Mensajería RabbitMQ (EP3):**
   - `pedidos360-consumer-service` — consumidores por dominio (`notifications`, `tickets`, `documents`) con **ACK manual** (`basicAck`/`basicNack` → DLQ).
   - `pedidos360-admin-service` — administrador de RabbitMQ (REST: crear/eliminar colas, exchanges, bindings + validación 400).
   - Config **100% centralizada** de nombres de colas/exchanges/DLX/routing keys en `application.yml` (cero hardcoding).
   - Topología: 3 colas de negocio + 3 DLQ + exchanges `direct` y `topic`.
4. **Infraestructura (EP4):**
   - `docker-compose` con **clúster RabbitMQ de 2 nodos** (`RABBITMQ_ERLANG_COOKIE` compartida + `rabbitmq_management`) y PostgreSQL.
   - Dockerfiles y `.gitignore` para todos los servicios.
5. **Despliegue en AWS** (ver sección 3).
6. **Skills de OpenCode** creadas: `frontend`, `backend`, `rabbitmq`, `cloud-infra` (en `.opencode/skills/`).

### Git

- `feature/rabbitmq-mensajeria` → PR #10 → mergeado en `develop`.
- Modelo de ramas: `feature/* → develop → main`.

---

## 2. Arquitectura

| Componente | Tecnología | Puerto |
|-----------|-----------|--------|
| Frontend | Angular 18 (MSAL) | 443 (nginx) |
| BFF | Spring Boot 3.3 | 8080 |
| pedidos-service | Spring Boot 3.3 (+ productor RabbitMQ) | 8081 |
| productos-service | Spring Boot 3.3 | 8082 |
| consumer-service | Spring Boot 3.3 (consumidores RabbitMQ) | 8083 |
| admin-service | Spring Boot 3.3 (admin RabbitMQ) | 8084 |
| RabbitMQ | clúster 2 nodos (`rabbitmq1`+`rabbitmq2`) | 5672 / 15672 |
| PostgreSQL | 16 | 5432 |
| API Gateway | AWS HTTP API + autorizador JWT (Azure AD) | 443 |

---

## 3. Despliegue AWS — estado actual

> ⚠️ Los valores de IP y API Gateway son **específicos de esta sesión de lab**. Ver sección 4 para reconstruir.

| Recurso | Valor |
|---------|-------|
| EC2 (IP pública) | `54.90.116.232` |
| API Gateway | `https://7vnpy8md20.execute-api.us-east-1.amazonaws.com` |
| Frontend | `https://54.90.116.232` (nginx + HTTPS autofirmado) |
| RabbitMQ Management | `http://54.90.116.232:15672` (guest/guest) |
| BFF | `http://54.90.116.232:8080` |
| Key pair | `vockey` (PEM en `~/.ssh/pedidos360-lab.pem`) |
| Cuenta / Región | `862834886823` / `us-east-1` |

**Verificado en vivo:** clúster RabbitMQ (2 nodos), 3 colas + 3 DLQ + exchanges `direct`/`topic`, flujo productor→consumidor, DLQ (NACK), admin REST (201/400/204), 401 del API Gateway y BFF, frontend servido, persistencia en PostgreSQL.

---

## 4. Cómo levantar de nuevo (si se reinicia el taller)

> **Guía completa y actualizada:** [`docs/RUNBOOK_DESPLIEGUE.md`](RUNBOOK_DESPLIEGUE.md) — 13 pasos de extremo a extremo (prerequisitos, clonar, credenciales, compilar, EC2, clúster RabbitMQ con *join automático*, servicios, nginx, API Gateway, checklist EP3/EP4 y gotchas).

> **Caveat importante:** los AWS Academy Learner Labs son **efímeros**. Al reiniciar la sesión cambian las credenciales, la IP y (normalmente) se eliminan los recursos (EC2, API Gateway). Hay que rehacer el despliegue. El **código NO se pierde** (está en GitHub).

### Resumen rápido (pasos)

1. **Validar credenciales** con `aws sts get-caller-identity`. Si da `InvalidClientTokenId`, el `aws_session_token` está **truncado** (debe terminar en `=`) → usar el botón **copiar 📋** del panel "Cloud Access", no seleccionar el texto a mano.
2. **Clonar y cambiar de rama:** `git clone https://github.com/victor99a/Cloud-Native.git && cd Cloud-Native && git checkout develop`.
3. **Compilar** los 5 JARs: `mvn -DskipTests package -f <servicio>/pom.xml` (bff, pedidos, productos, consumer, admin).
4. **EC2:** security group (puertos `22,80,443,8080,8081-8084,15672,5672`) + AMI *Amazon Linux 2023* `t3.medium` + `userdata` que instala docker/git/java/nginx.
5. **Subir** los 5 JARs, `docker-compose.yml` y `infra/apps/rabbitmq/rabbitmq.conf` con `scp`.
6. **Levantar RabbitMQ clúster + PostgreSQL:** `docker-compose up -d rabbitmq1 rabbitmq2 postgres`. El nodo 2 se une **solo** por *peer discovery* (ya NO hace falta el join manual con `CLUSTER_WITH`). Verificar con `rabbitmq-diagnostics cluster_status` (2 nodos).
7. **Correr los 5 servicios** con `SPRING_PROFILES_ACTIVE=dev` (para que `ddl-auto:update` cree las tablas).
8. **Frontend:** compilar con `environment.prod.ts` actualizado (IP + API Gateway nuevos), servir con **nginx + HTTPS** (obligatorio para MSAL).
9. **API Gateway + autorizador JWT** de Azure AD (`issuer` `.../v2.0`, `audience` `b4a1d505-...`).
10. **Registrar redirect URI** `https://<IP>` en Azure AD y recompilar/re-subir el frontend.

### Paso 1 — Obtener credenciales del lab y configurar AWS CLI

```powershell
# Del panel del lab: Access Key ID / Secret / Session Token
aws configure set aws_access_key_id "<KEY>"
aws configure set aws_secret_access_key "<SECRET>"
aws configure set aws_session_token "<TOKEN>"
aws configure set region us-east-1
aws sts get-caller-identity   # verificar
```

### Paso 2 — (Opcional) IP elástica para que la IP no cambie

Ver **sección 5**.

### Paso 3 — Crear Security Group y lanzar la EC2

```powershell
# 1) SG con puertos 22, 80, 443, 8080-8084, 15672, 5672
$sgId = aws ec2 create-security-group --group-name pedidos360-sg --description "Pedidos360" --vpc-id <VPC_ID> --query 'GroupId' --output text
aws ec2 authorize-security-group-ingress --group-id $sgId --protocol tcp --port 22 --cidr 0.0.0.0/0
# ... repetir para 80, 443, 8080, 8081, 8082, 8083, 8084, 15672, 5672

# 2) AMI Amazon Linux 2023
$ami = aws ssm get-parameters --names /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 --query 'Parameters[0].Value' --output text

# 3) Lanzar con user-data que instala Docker/Java/nginx/git
aws ec2 run-instances --image-id $ami --instance-type t3.medium --key-name vockey --security-group-ids $sgId --associate-public-ip-address --user-data file://userdata.sh
```

`userdata.sh`:
```bash
#!/bin/bash
dnf update -y
dnf install -y docker git nginx java-21-amazon-corretto
systemctl enable docker && systemctl start docker
usermod -aG docker ec2-user
systemctl enable nginx && systemctl start nginx
# docker-compose (binario standalone)
curl -sL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
```

### Paso 4 — Compilar localmente y subir los artefactos

```powershell
# Backend (5 JARs)
mvn -DskipTests package -f pedidos360-bff/pom.xml
mvn -DskipTests package -f pedidos360-pedidos-service/pom.xml
mvn -DskipTests package -f pedidos360-productos-service/pom.xml
mvn -DskipTests package -f pedidos360-consumer-service/pom.xml
mvn -DskipTests package -f pedidos360-admin-service/pom.xml

# Frontend (producción)
cd pedidos360-frontend && npm install && npm run build -- --configuration production
```

Subir a la EC2 (JARs a `~/deploy/app/`, frontend `dist/.../browser` a `~/frontend/`):
```powershell
scp -i ~/.ssh/pedidos360-lab.pem -r <jars> ec2-user@<IP>:~/deploy/app/
scp -i ~/.ssh/pedidos360-lab.pem -r <browser> ec2-user@<IP>:~/frontend
```

### Paso 5 — Levantar RabbitMQ (clúster) + PostgreSQL con docker-compose

Subir `docker-compose.yml` (postgres + `rabbitmq1` + `rabbitmq2` con la misma `RABBITMQ_ERLANG_COOKIE`) y:
```bash
docker-compose up -d
# Unir el nodo 2 al clúster si CLUSTER_WITH no lo hizo solo:
docker exec rabbitmq2 rabbitmqctl stop_app
docker exec rabbitmq2 rabbitmqctl join_cluster rabbit@rabbitmq1
docker exec rabbitmq2 rabbitmqctl start_app
```

### Paso 6 — Correr los 5 servicios (nativo, perfil `dev`)

`run.sh` (con `SPRING_PROFILES_ACTIVE=dev` para que `ddl-auto:update` cree las tablas):
```bash
export SPRING_PROFILES_ACTIVE=dev
export DB_HOST=localhost DB_PORT=5432 DB_NAME=pedidos360 DB_USERNAME=postgres DB_PASSWORD=postgres
export RABBITMQ_HOST=localhost RABBITMQ_PORT=5672 RABBITMQ_USERNAME=guest RABBITMQ_PASSWORD=guest
export AZURE_ISSUER_URI="https://login.microsoftonline.com/840f00c4-5744-42d5-8827-ed48c11ff4d4/v2.0"
export AZURE_AUDIENCE="b4a1d505-4e63-4d74-ad9a-5f7baf82b7dd"
export DOWNSTREAM_PEDIDOS_URL="http://localhost:8081"
export DOWNSTREAM_PRODUCTOS_URL="http://localhost:8082"
nohup java -jar app/pedidos.jar  > logs/pedidos.log  2>&1 &
nohup java -jar app/productos.jar > logs/productos.log 2>&1 &
nohup java -jar app/consumer.jar > logs/consumer.log 2>&1 &
nohup java -jar app/admin.jar    > logs/admin.log    2>&1 &
nohup java -jar app/bff.jar      > logs/bff.log      2>&1 &
```

### Paso 7 — Frontend en nginx + HTTPS (autofirmado)

```bash
sudo mkdir -p /etc/nginx/ssl
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /etc/nginx/ssl/server.key -out /etc/nginx/ssl/server.crt -subj "/CN=<IP>"
# nginx.conf: redirect 80->443, y server 443 con ssl + try_files $uri /index.html
sudo cp ~/frontend/* /usr/share/nginx/html/
sudo systemctl restart nginx
```

> **Por qué HTTPS:** MSAL usa `crypto.subtle`, que solo existe en `https://` o `localhost`. En HTTP plano el frontend queda en blanco.

### Paso 8 — Crear API Gateway con autorizador JWT (Azure AD)

```powershell
$apiId = aws apigatewayv2 create-api --name pedidos360-api --protocol-type HTTP --query 'ApiId' --output text
$authId = aws apigatewayv2 create-authorizer --api-id $apiId --name azure-ad-jwt --authorizer-type JWT --identity-source '$request.header.Authorization' --jwt-configuration 'Issuer=https://login.microsoftonline.com/840f00c4-5744-42d5-8827-ed48c11ff4d4/v2.0,Audience=b4a1d505-4e63-4d74-ad9a-5f7baf82b7dd' --query 'AuthorizerId' --output text
$intId = aws apigatewayv2 create-integration --api-id $apiId --integration-type HTTP_PROXY --integration-method ANY --integration-uri "http://<IP>:8080/api/{proxy}" --payload-format-version 1.0 --query 'IntegrationId' --output text
aws apigatewayv2 create-route --api-id $apiId --route-key 'ANY /api/{proxy+}' --target "integrations/$intId" --authorization-type JWT --authorizer-id $authId
aws apigatewayv2 create-stage --api-id $apiId --stage-name '$default' --auto-deploy
```

### Paso 9 — Actualizar las URLs del frontend y el redirect URI

1. En `pedidos360-frontend/src/app/environments/environment.prod.ts`:
   - `apiGatewayUrl` → `https://<API_ID>.execute-api.us-east-1.amazonaws.com/api`
   - `redirectUri` / `postLogoutRedirectUri` → `https://<IP>`
2. Recompilar y redeployar el frontend.
3. Registrar el redirect URI `https://<IP>` en **Azure AD → App registrations → Pedidos360 → Authentication → SPA**.

---

## 5. IP elástica (para que la IP no cambie entre reinicios)

### ⚠️ ¿La IP cambia al detener/reiniciar el taller?

**Sí — por defecto la IP pública CAMBIA.** Al detener e iniciar la instancia (o reiniciar el lab), la IP pública autoasignada se libera y se asigna una nueva. Si pasa, hay que actualizar: el redirect URI en Azure AD, la integración del API Gateway, `environment.prod.ts` y el certificado de nginx.

**Para que NO cambie, usa una IP elástica** (abajo). Con la EIP asociada, la IP se mantiene al detener/iniciar la instancia.

### Cómo asignar la IP elástica

```powershell
# 1) Asignar una IP elástica (devuelve AllocationId)
aws ec2 allocate-address --domain vpc --region us-east-1

# 2) Asociarla a la instancia
aws ec2 associate-address --instance-id i-XXXXXXXX --allocation-id eipalloc-XXXXXXXX --region us-east-1
```

**Caveat:** la EIP evita el cambio de IP si la instancia se **detiene/inicia** (no se termina). Pero si el **lab se reinicia por completo y termina la instancia**, habrá que lanzar una nueva y **reasociar** la EIP (la asignación suele conservarse en la cuenta durante el lab, pero no es garantía). Ante un reinicio total: repetir el runbook de la sección 4 y reasociar la EIP si existe.

---

## 6. Estado de los pendientes (actualizado)

- ✅ **Redirect URI en Azure AD** (`https://54.90.116.232`): registrado.
- ✅ **Claim `aud`**: verificado — el token trae `aud = b4a1d505-...` (SIN prefijo `api://`); el Gateway y el BFF ya usan ese valor.
- ✅ **CORS**: configurado en el API Gateway (ruta OPTIONS sin autorizador) + `CorsConfig` del BFF permite cualquier origen.
- ✅ **Puertos de servicios cerrados** (8081-8084, 5672): solo quedan abiertos 22, 80, 443, 8080 y 15672.
- ⚠️ Los archivos `environment.ts`/`environment.prod.ts` son específicos del lab: **no versionar** las IPs temporales.

---

## 7. Guion de demostración EP4 (11 puntos, 5-10 min)

1. **API Manager en la nube** — AWS Console → API Gateway → mostrar la HTTP API **`pedidos360-api`** (id `7vnpy8md20`) activa. URL base: `https://7vnpy8md20.execute-api.us-east-1.amazonaws.com`.
2. **Configuración de endpoints** — En el API Gateway: ruta `ANY /api/{proxy+}` → integración HTTP_PROXY → `http://54.90.116.232:8080/api/{proxy}`; y el autorizador **`azure-ad-jwt`** (JWT, issuer de Azure AD).
3. **Frontend consume vía API Manager** — DevTools → Network → recargar → mostrar que las peticiones van a `https://7vnpy8md20.execute-api...` (nunca a IPs directas de los servicios).
4. **Validación JWT (200 vs 401)** —
   ```powershell
   # Sin token -> 401
   curl -s -o /dev/null -w "%{http_code}" https://7vnpy8md20.execute-api.us-east-1.amazonaws.com/api/pedidos
   # Con token válido (app logueada) -> 200
   ```
5. **IDaaS tenant + usuarios** — Azure Portal → Microsoft Entra ID → tenant `840f00c4...`, app **Pedidos360**, y **Users** (cuenta con rol `ADMIN`).
6. **Login OAuth2/OIDC en vivo** — `https://54.90.116.232` → "Iniciar sesión con Microsoft" → redirección a Microsoft → vuelta con JWT (DevTools → Application → Local Storage → caché MSAL).
7. **Despliegue backend/frontend** — mostrar la EC2 `54.90.116.232` y la integración frontend → Gateway → BFF → servicios.
8. **Clúster RabbitMQ (2 nodos)** — `http://54.90.116.232:15672` (guest/guest) → **Overview** → 2 nodos: `rabbit@rabbitmq1` + `rabbit@rabbitmq2`.
9. **3 colas + 3 DLQs + fallo** — pestaña **Queues** → `notificaciones`, `tickets`, `documentos` + sus `.dlq`. Forzar un fallo (mensaje vacío) → aparece en `notificaciones.dlq`.
10. **Exchanges direct y topic** — pestaña **Exchanges** → `pedidos360.direct` (direct) y `pedidos360.topic` (topic).
11. **docker-compose** — por SSH en la EC2: `cd ~/deploy && docker-compose ps` → postgres + `rabbitmq1` + `rabbitmq2` corriendo vía docker-compose.

---

## 8. Iteraciones y gotchas del despliegue (lecciones aprendidas)

Errores que aparecieron durante el despliegue real y su solución. Revisar esta sección **antes y durante** el levantamiento en otro taller.

### 8.1 Audience (`aud`) del JWT
- El token de Azure AD trae `aud = b4a1d505-4e63-4d74-ad9a-5f7baf82b7dd` (**SIN** el prefijo `api://`), no `api://b4a1d505-...`.
- Configurar el BFF (`AZURE_AUDIENCE`) y el autorizador del API Gateway con `b4a1d505-...` (GUID pelado).
- Verificar el claim `aud` con **jwt.ms** tras el primer login; si hay 401 con token válido, es esto.

### 8.2 CORS (preflight OPTIONS → 401/403)
- El `OPTIONS` (preflight) no lleva token, por lo que el autorizador JWT del Gateway lo rechazaba (401); luego el BFF rechazaba el origen (403 "Invalid CORS request").
- Fix en **3 partes**:
  1. CORS en el API Gateway: `AllowOrigins=*`, métodos (incl. `OPTIONS`), headers `authorization,content-type`.
  2. Ruta `OPTIONS /api/{proxy+}` **sin** autorizador.
  3. `CorsConfig.java` del BFF con `setAllowedOriginPatterns(List.of("*"))` (la IP es efímera).

### 8.3 MSAL exige HTTPS
- MSAL usa `crypto.subtle`, que solo existe en `https://` o `localhost`. En `http://<IP>` el frontend queda en **pantalla en blanco**.
- Fix: HTTPS en nginx con certificado autofirmado + redirect `80 → 443`.

### 8.4 Clúster RabbitMQ: join manual
- El env `CLUSTER_WITH` no unió el nodo 2 automáticamente. Join manual:
  ```bash
  docker exec rabbitmq2 rabbitmqctl stop_app
  docker exec rabbitmq2 rabbitmqctl join_cluster rabbit@rabbitmq1
  docker exec rabbitmq2 rabbitmqctl start_app
  ```

### 8.5 docker-compose en Amazon Linux 2023
- `dnf install docker-compose-plugin` falla ("No match for argument"). Usar el binario standalone:
  ```bash
  curl -sL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose
  chmod +x /usr/local/bin/docker-compose
  ```

### 8.6 Esquema de BD (`ddl-auto`)
- Los servicios usan `ddl-auto: validate` en el perfil base. Correrlos con `SPRING_PROFILES_ACTIVE=dev` para que `ddl-auto: update` cree las tablas automáticamente.

### 8.7 Puertos de seguridad (hardening)
- **No exponer** los servicios (8081-8084) ni AMQP (5672) a internet: solo el BFF valida JWT. Dejar abiertos: `22`, `80`, `443`, `8080` (BFF, lo usa el API Gateway) y `15672` (RabbitMQ Management para la demo).
- Evita los escaneos de internet que ensucian los logs.

### 8.8 IP cambia al reiniciar el taller
- Ver sección 5. Sin IP elástica, la IP cambia → hay que actualizar redirect URI (Azure), integración del Gateway, `environment.prod.ts` y certificado nginx.

### 8.9 URLs del frontend (por lab)
- En cada lab nuevo actualizar `environment.prod.ts` y `environment.ts` (`apiGatewayUrl` + `redirectUri`) y **reconstruir** el frontend. Estas IPs son temporales: **no versionarlas**.

### 8.10 Iconos Material Symbols
- Los iconos se cargan desde Google Fonts CDN (`fonts.googleapis.com`). Requieren internet en el navegador.
