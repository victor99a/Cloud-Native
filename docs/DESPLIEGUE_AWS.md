# Despliegue en AWS (AWS Academy Learner Lab)

Arquitectura desplegada en la nube para la EP6. Todo corre en la cuenta del Learner Lab
(región `us-east-1`).

```
Navegador ──HTTPS──► Amazon API Gateway (HTTP API "pedidos360-api-manager")  ◄── API Manager
                       │  GET /, GET /{proxy+}  ──────────────► EC2 apps :4200  (frontend nginx)
                       │  ANY /api/{proxy+}  [JWT authorizer] ─► EC2 apps :8080  (Spring Gateway → BFF → microservicios)
                       │
Navegador ──HTTPS──► Amazon Cognito (User Pool "pedidos360-tenant")            ◄── IDaaS (tenant + usuarios)
                       Hosted UI: login OAuth 2.0 / OIDC, Authorization Code + PKCE

EC2 "pedidos360-apps"      (t3.large) : PostgreSQL + 8 microservicios Spring Boot + frontend
EC2 "pedidos360-messaging" (t3.large) : 3 Zookeeper + 3 brokers Kafka + Kafka UI + RabbitMQ (2 nodos)
```

> El Learner Lab limita las instancias a `t3.large` (8 GB), por eso la plataforma se divide en
> dos hosts. Los microservicios llegan a Kafka/RabbitMQ por la **IP privada** del host
> `messaging` (estable aunque se apague el lab).

## Recursos creados

| Recurso | Nombre / ID | Rol en la rúbrica |
|---------|-------------|-------------------|
| API Gateway (HTTP API) | `pedidos360-api-manager` — `https://136xrpjdoj.execute-api.us-east-1.amazonaws.com` | API Manager; valida JWT (rechaza inválidos con 401) |
| JWT authorizer | `cognito-jwt` (issuer Cognito, audience = client id) | Validación de JWT en el API Manager |
| Cognito User Pool | `pedidos360-tenant` — `us-east-1_NUy3OxPyD` | Tenant del IDaaS |
| Cognito App client | `pedidos360-spa` — `3co912n6as17utm5q0a07upsrl` (público, PKCE) | Login OIDC del frontend |
| Cognito domain | `https://pedidos360-381492248228.auth.us-east-1.amazoncognito.com` | Hosted UI |
| Grupos | `ADMIN`, `OPERADOR`, `CLIENTE` | Roles (claim `cognito:groups`) |
| EC2 | `pedidos360-apps` (EIP `52.45.200.182`), `pedidos360-messaging` (EIP `44.213.34.232`) | Backend + frontend desplegados |
| Security group | `pedidos360-sg` | 8080/4200 abiertos (API Gateway); 22/8091/15672 solo IP admin |

Las **IPs elásticas** mantienen las direcciones públicas fijas entre sesiones del lab, así las
integraciones del API Gateway no se rompen al reiniciar.

## Volver a levantar todo antes de presentar

1. Iniciar el Learner Lab y copiar las credenciales nuevas a `~/.aws/credentials`.
2. Las EC2 arrancan solas con el lab. Si están detenidas:
   `aws ec2 start-instances --instance-ids i-0b30bbb7068807090 i-0890dc45c3a3a3adc`
3. Los contenedores tienen `restart: unless-stopped`, así que vuelven solos. Para verificar:
   ```bash
   ssh -i ~/.ssh/labsuser.pem ec2-user@44.213.34.232 \
     'cd ~/pedidos360/infra/aws && docker compose -f docker-compose.messaging.yml ps'
   ssh -i ~/.ssh/labsuser.pem ec2-user@52.45.200.182 \
     'cd ~/pedidos360/infra/aws && docker compose -f docker-compose.apps.yml ps'
   ```
   Si algo no está arriba: `docker compose -f <archivo> --env-file .env up -d` (primero
   `messaging`, luego `apps`).
4. Si cambió tu IP pública, actualiza el security group para SSH / Kafka UI / RabbitMQ UI.

## Guion de demo (rúbrica EP6)

| Punto de la rúbrica | Qué mostrar |
|---------------------|-------------|
| Instancia de API Manager | Consola → API Gateway → `pedidos360-api-manager` (rutas, integraciones, stage `$default`). |
| Configuración que llama al backend | Ruta `ANY /api/{proxy+}` → integración HTTP `http://52.45.200.182:8080/api/{proxy}`. |
| Frontend consume vía API Manager | Abrir la URL del API Gateway, iniciar sesión y navegar; en DevTools → Network todas las llamadas van a `…execute-api…/api/...`. |
| API Manager valida JWT | `curl -i <API>/api/pedidos` → **401**; con token inválido → **401**; con token válido → **200** (ver abajo). |
| Tenant IDaaS + usuarios | Consola → Cognito → User pools → `pedidos360-tenant` → Users / Groups. |
| OAuth 2.0 / OIDC + JWT válido | Botón "Iniciar sesión" → Hosted UI de Cognito → vuelta con `?code=` → token en `Authorization: Bearer`. Decodificar en jwt.io. |
| Backend y frontend desplegados | Consola EC2 (2 instancias running) + `docker compose ps`. |
| RabbitMQ 2 nodos en clúster | `http://44.213.34.232:15672` → Overview → Nodes (rabbit@rabbitmq1, rabbit@rabbitmq2). |
| 3 colas + DLQ, exchanges direct/topic | RabbitMQ UI → Queues / Exchanges. |
| docker-compose en la nube | `docker compose -f docker-compose.messaging.yml ps` por SSH. |
| Kafka UI: 2 tópicos 3×3 | `http://44.213.34.232:8091` → Topics. |

### Probar la validación de JWT desde la terminal

```bash
API=https://136xrpjdoj.execute-api.us-east-1.amazonaws.com
curl -i $API/api/pedidos                                         # 401 sin token
curl -i -H "Authorization: Bearer abc.def.ghi" $API/api/pedidos   # 401 token inválido

TOKEN=$(aws cognito-idp initiate-auth --auth-flow USER_PASSWORD_AUTH \
  --client-id 3co912n6as17utm5q0a07upsrl \
  --auth-parameters USERNAME=admin,PASSWORD='<clave>' \
  --query AuthenticationResult.AccessToken --output text)
curl -i -H "Authorization: Bearer $TOKEN" $API/api/pedidos        # 200
```
