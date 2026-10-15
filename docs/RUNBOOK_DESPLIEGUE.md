# Runbook — Despliegue completo de Pedidos360 en AWS (desde cero)

> Guía paso a paso para levantar todo el sistema en una máquina nueva.
> Stack: Angular 18 + 5 microservicios Spring Boot 3.3 (Java 21) + RabbitMQ (clúster 2 nodos) + PostgreSQL + Azure AD (Entra ID) + AWS (EC2 + API Gateway).

---

## 0. Arquitectura y puertos

| Componente | Puerto | Notas |
|---|---|---|
| `pedidos360-frontend` | 443 (nginx) | SPA Angular 18, login MSAL |
| `pedidos360-bff` | 8080 | Valida JWT y enruta a servicios |
| `pedidos360-pedidos-service` | 8081 | CRUD pedidos + **productor** RabbitMQ |
| `pedidos360-productos-service` | 8082 | CRUD productos |
| `pedidos360-consumer-service` | 8083 | **Consumidores** RabbitMQ |
| `pedidos360-admin-service` | 8084 | **Administrador** RabbitMQ (REST) |
| PostgreSQL | 5432 | (mapeado 5433 en compose) |
| RabbitMQ nodo1 | 5672 / 15672 | clúster |
| RabbitMQ nodo2 | 5673 / 15673 | clúster |

**Topología de mensajería:** exchanges `pedidos360.topic` (topic), `pedidos360.direct` (direct), `pedidos360.dlx` (DLX direct). 3 colas de negocio + 3 DLQ. ACK manual (`basicAck`/`basicNack`→DLQ).

---

## 1. Prerequisitos en la máquina nueva

Instalar:

- **Java 21** (Adoptium/Corretto)
- **Maven 3.9+**
- **Node 18+** y npm
- **Git**
- **AWS CLI v2**
- Cliente **SSH** (OpenSSH, `scp`, `ssh`)

> Docker NO es obligatorio en tu máquina local: los JARs se compilan con Maven y se suben a la EC2. Docker solo se usa DENTRO de la EC2 (para RabbitMQ/Postgres) o no (si levantas RabbitMQ nativo).

---

## 2. Obtener el código

**Opción A — clonar desde GitHub** (requiere que los cambios estén pusheados):

```bash
git clone https://github.com/victor99a/Cloud-Native.git
cd Cloud-Native
git checkout feat/ui-icons-cors
```

**Opción B — copiar la carpeta** (si los cambios aún no están en GitHub): copia toda la carpeta `Cloud-Native` (incluye los refactors de RabbitMQ y `docs/`). Úsala si no haces push antes de cambiar de equipo.

> ⚠️ Verifica que existan estos archivos (son los refactors recientes):
> - `infra/apps/rabbitmq/rabbitmq.conf`
> - `pedidos360-consumer-service/.../config/RabbitMQConfig.java` (con el bean `rabbitListenerContainerFactory`)
> - `pedidos360-pedidos-service/.../messaging/RabbitMQProducerConfig.java` (con `RabbitTemplate` + confirms)

---

## 3. Configurar credenciales AWS y VALIDAR

Crea/edita `~/.aws/credentials`:

```ini
[default]
aws_access_key_id=ASIA...
aws_secret_access_key=...
aws_session_token=IQoJ...
```

Y `~/.aws/config`:

```ini
[default]
region=us-east-1
output=json
```

**Valida SIEMPRE antes de continuar** (esto te ahorra perder tiempo):

```bash
aws sts get-caller-identity
```

Debe devolver tu cuenta (`862834886823`) sin errores.

> ⚠️ **Ojo con el token truncado:** si te da `InvalidClientTokenId`, revisa que el `aws_session_token` **termine en `=`**. Si termina en otra cosa (ej. `...3S1`), está incompleto → usa el botón **copiar 📋** del panel "Cloud Access" (no selecciones el texto a mano).

---

## 4. Datos de Azure AD (estables, no cambian)

| Clave | Valor |
|---|---|
| tenantId | `840f00c4-5744-42d5-8827-ed48c11ff4d4` |
| clientId (API app) | `b4a1d505-4e63-4d74-ad9a-5f7baf82b7dd` |
| authority | `https://login.microsoftonline.com/840f00c4-5744-42d5-8827-ed48c11ff4d4` |
| issuer-uri (BFF/Gateway) | `https://login.microsoftonline.com/840f00c4-5744-42d5-8827-ed48c11ff4d4/v2.0` |
| audience (BFF/Gateway) | `b4a1d505-4e63-4d74-ad9a-5f7baf82b7dd` |
| scopes (frontend) | `api://b4a1d505-4e63-4d74-ad9a-5f7baf82b7dd/access_as_user` |

---

## 5. Compilar el backend (5 JARs)

```bash
mvn -DskipTests package -f pedidos360-bff/pom.xml
mvn -DskipTests package -f pedidos360-pedidos-service/pom.xml
mvn -DskipTests package -f pedidos360-productos-service/pom.xml
mvn -DskipTests package -f pedidos360-consumer-service/pom.xml
mvn -DskipTests package -f pedidos360-admin-service/pom.xml
```

Los JARs quedan en `*/target/*.jar`.

---

## 6. Lanzar la EC2

### 6.1 Security Group

```bash
SG=$(aws ec2 create-security-group --group-name pedidos360-sg --description "Pedidos360" --query 'GroupId' --output text)
for p in 22 80 443 8080 8081 8082 8083 8084 15672 5672; do
  aws ec2 authorize-security-group-ingress --group-id $SG --protocol tcp --port $p --cidr 0.0.0.0/0
done
```

> Para producción/lab: deja abiertos solo `22`, `80`, `443`, `8080`, `15672`. Los puertos `8081-8084` y `5672` no deberían quedar expuestos a internet (solo los usa el BFF/Gateway internamente). Para la demo es aceptable abrirlos, pero el checklist EP4 pide hardening.

### 6.2 AMI y launch

```bash
AMI=$(aws ssm get-parameters --names /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 --query 'Parameters[0].Value' --output text)
aws ec2 run-instances \
  --image-id $AMI \
  --instance-type t3.medium \
  --key-name <NOMBRE_DE_TU_KEYPAIR> \
  --security-group-ids $SG \
  --associate-public-ip-address \
  --user-data file://userdata.sh \
  --query 'Instances[0].InstanceId' --output text
```

> En AWS Academy Learner Lab, si ya hay un key pair preconfigurado (`vockey` o `labsuser`), usa ese nombre. Si no, importa tu llave: `aws ec2 import-key-pair --key-name labsuser --public-key-material fileb://labsuser.pem.pub`.

### 6.3 `userdata.sh` (instala Docker, git, Java, nginx)

```bash
#!/bin/bash
dnf update -y
dnf install -y docker git nginx java-21-amazon-corretto
systemctl enable docker && systemctl start docker
usermod -aG docker ec2-user
systemctl enable nginx && systemctl start nginx
curl -sL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
```

### 6.4 Obtener la IP pública

```bash
aws ec2 describe-instances --instance-ids <INSTANCE_ID> --query 'Reservations[0].Instances[0].PublicIpAddress' --output text
```

Anota esa IP (la llamaremos `<IP>`).

---

## 7. Subir los artefactos a la EC2

```bash
# Crear carpetas remotas
ssh -i labsuser.pem ec2-user@<IP> "mkdir -p ~/deploy/app ~/deploy/logs ~/deploy/infra"

# Subir JARs
scp -i labsuser.pem pedidos360-bff/target/*.jar ec2-user@<IP>:~/deploy/app/bff.jar
scp -i labsuser.pem pedidos360-pedidos-service/target/*.jar ec2-user@<IP>:~/deploy/app/pedidos.jar
scp -i labsuser.pem pedidos360-productos-service/target/*.jar ec2-user@<IP>:~/deploy/app/productos.jar
scp -i labsuser.pem pedidos360-consumer-service/target/*.jar ec2-user@<IP>:~/deploy/app/consumer.jar
scp -i labsuser.pem pedidos360-admin-service/target/*.jar ec2-user@<IP>:~/deploy/app/admin.jar

# Subir docker-compose + config del clúster
scp -i labsuser.pem infra/apps/docker-compose.yml ec2-user@<IP>:~/deploy/infra/
scp -i labsuser.pem infra/apps/rabbitmq/rabbitmq.conf ec2-user@<IP>:~/deploy/infra/
```

---

## 8. Levantar RabbitMQ (clúster 2 nodos) + PostgreSQL

El `docker-compose.yml` ya trae el clúster con **peer discovery clásico** (join automático) y el healthcheck de `cluster_status`.

> Necesitas el `rabbitmq.conf` en `infra/apps/rabbitmq/` (móntalo en la EC2 en la misma ruta relativa: `~/deploy/infra/rabbitmq/rabbitmq.conf`).

```bash
ssh -i labsuser.pem ec2-user@<IP>
cd ~/deploy/infra
mkdir -p rabbitmq
mv rabbitmq.conf rabbitmq/   # si lo subiste suelto
docker-compose up -d rabbitmq1 rabbitmq2 postgres

# Verificar clúster (debe listar 2 nodos)
docker-compose exec rabbitmq1 rabbitmq-diagnostics cluster_status
```

Consola de gestión: `http://<IP>:15672` (guest/guest).

---

## 9. Correr los 5 servicios (nativo, perfil dev)

```bash
ssh -i labsuser.pem ec2-user@<IP>
cd ~/deploy
export SPRING_PROFILES_ACTIVE=dev
export DB_HOST=localhost DB_PORT=5432 DB_NAME=pedidos360 DB_USERNAME=postgres DB_PASSWORD=postgres
export RABBITMQ_HOST=localhost RABBITMQ_PORT=5672 RABBITMQ_USERNAME=guest RABBITMQ_PASSWORD=guest
export AZURE_ISSUER_URI="https://login.microsoftonline.com/840f00c4-5744-42d5-8827-ed48c11ff4d4/v2.0"
export AZURE_AUDIENCE="b4a1d505-4e63-4d74-ad9a-5f7baf82b7dd"
export DOWNSTREAM_PEDIDOS_URL="http://localhost:8081"
export DOWNSTREAM_PRODUCTOS_URL="http://localhost:8082"

nohup java -jar app/pedidos.jar    > logs/pedidos.log    2>&1 &
nohup java -jar app/productos.jar  > logs/productos.log  2>&1 &
nohup java -jar app/consumer.jar   > logs/consumer.log   2>&1 &
nohup java -jar app/admin.jar      > logs/admin.log      2>&1 &
nohup java -jar app/bff.jar        > logs/bff.log        2>&1 &
```

> Nota: el perfil `dev` usa `ddl-auto: update` (crea las tablas automáticamente). Verifica logs: `tail -f logs/*.log`.

---

## 10. Frontend en nginx + HTTPS (autofirmado)

### 10.1 Compilar el frontend (con las URLs correctas)

En tu máquina local, edita `pedidos360-frontend/src/app/environments/environment.prod.ts`:

```ts
apiGatewayUrl: 'https://<API_ID>.execute-api.us-east-1.amazonaws.com/api',
redirectUri: 'https://<IP>',
postLogoutRedirectUri: 'https://<IP>',
```

> Si aún no tienes el API Gateway, compila primero con una URL temporal y recompila al final (el `apiGatewayUrl` se puede reemplazar también en el bundle si usas el mismo texto). Lo más limpio: crear el API Gateway **antes** de compilar.

```bash
cd pedidos360-frontend
npm install
npm run build -- --configuration production
```

El build queda en `dist/pedidos360-frontend/browser/`.

### 10.2 Subir y servir con nginx

```bash
scp -i labsuser.pem -r dist/pedidos360-frontend/browser/* ec2-user@<IP>:~/frontend/
ssh -i labsuser.pem ec2-user@<IP>
sudo mkdir -p /etc/nginx/ssl
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/nginx/ssl/server.key -out /etc/nginx/ssl/server.crt -subj "/CN=<IP>"
sudo cp -r ~/frontend/* /usr/share/nginx/html/
sudo systemctl restart nginx
```

> Configura nginx para: redirigir `80 → 443`, servir con SSL y `try_files $uri /index.html` (SPA). **HTTPS es obligatorio** para MSAL (`crypto.subtle` solo existe en https o localhost).

---

## 11. API Gateway + autorizador JWT

```bash
API_ID=$(aws apigatewayv2 create-api --name pedidos360-api --protocol-type HTTP --query 'ApiId' --output text)

AUTH_ID=$(aws apigatewayv2 create-authorizer --api-id $API_ID --name azure-ad-jwt \
  --authorizer-type JWT --identity-source '$request.header.Authorization' \
  --jwt-configuration "Issuer=https://login.microsoftonline.com/840f00c4-5744-42d5-8827-ed48c11ff4d4/v2.0,Audience=b4a1d505-4e63-4d74-ad9a-5f7baf82b7dd" \
  --query 'AuthorizerId' --output text)

INT_ID=$(aws apigatewayv2 create-integration --api-id $API_ID --integration-type HTTP_PROXY \
  --integration-method ANY --integration-uri "http://<IP>:8080/api/{proxy}" \
  --payload-format-version 1.0 --query 'IntegrationId' --output text)

aws apigatewayv2 create-route --api-id $API_ID --route-key 'ANY /api/{proxy+}' \
  --target "integrations/$INT_ID" --authorization-type JWT --authorizer-id $AUTH_ID

# Ruta OPTIONS sin autorizador (para CORS preflight)
aws apigatewayv2 create-route --api-id $API_ID --route-key 'OPTIONS /api/{proxy+}' \
  --target "integrations/$INT_ID"

aws apigatewayv2 create-stage --api-id $API_ID --stage-name '$default' --auto-deploy
```

Anota la URL: `https://<API_ID>.execute-api.us-east-1.amazonaws.com`.

---

## 12. Cerrar el círculo (frontend + Azure AD)

1. Registra el **redirect URI** `https://<IP>` en **Azure AD → App registrations → Pedidos360 → Authentication → SPA**.
2. Actualiza `environment.prod.ts` con el `apiGatewayUrl` definitivo y recompila (paso 10.1).
3. Re-subir y recargar nginx.

---

## 13. Verificación final (checklist EP3/EP4)

1. `aws sts get-caller-identity` → cuenta correcta.
2. Clúster RabbitMQ: `docker-compose exec rabbitmq1 rabbitmq-diagnostics cluster_status` → **2 nodos**.
3. Management UI `http://<IP>:15672` → 3 colas + 3 DLQ + exchanges `direct`/`topic`.
4. Productor→consumidor: `POST https://<API_ID>.../api/pedidos` (con JWT) → ver log del `consumer-service`.
5. DLQ: publicar mensaje inválido → aparece en la `.dlq`.
6. Admin REST:
   ```bash
   curl -X POST http://<IP>:8084/api/v1/admin/queues -H "Content-Type: application/json" -d '{"name":"demo.queue"}'
   curl -X DELETE http://<IP>:8084/api/v1/admin/queues/demo.queue
   ```
   (nombre vacío → HTTP 400).
7. Frontend `https://<IP>` → login Azure AD → JWT.
8. API Gateway: sin token → 401; con token → 200.

---

## Anexo — Gotchas conocidos

- **Audience (`aud`)**: el JWT trae `b4a1d505-...` SIN prefijo `api://`. Usa el GUID pelado en BFF y Gateway.
- **CORS**: ruta `OPTIONS` sin autorizador en el Gateway + `CorsConfig` del BFF.
- **MSAL exige HTTPS**: en HTTP el frontend queda en blanco.
- **Clúster**: con el nuevo `rabbitmq.conf` (peer discovery) el nodo2 se une solo; si falla, join manual:
  ```bash
  docker exec rabbitmq2 rabbitmqctl stop_app
  docker exec rabbitmq2 rabbitmqctl join_cluster rabbit@rabbitmq1
  docker exec rabbitmq2 rabbitmqctl start_app
  ```
- **docker-compose en AL2023**: `dnf install docker-compose-plugin` falla; usar el binario standalone (ver userdata).
- **ddl-auto**: servicios usan `validate` en base; correr con `SPRING_PROFILES_ACTIVE=dev` para que `update` cree tablas.
- **IP efímera**: al reiniciar el lab cambia la IP pública. Usar **IP elástica** para que no cambie, y actualizar redirect URI + integración del Gateway + `environment.prod.ts`.
