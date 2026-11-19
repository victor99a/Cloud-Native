#!/usr/bin/env bash
# =============================================================================
# Despliegue IDEMPOTENTE de Pedidos360 en AWS (pensado para AWS Academy Learner Lab).
#
# Sirve para los 3 casos, con el MISMO comando:
#   1. Cuenta nueva (otro taller)      -> crea toda la infraestructura desde cero.
#   2. Misma cuenta, sesión nueva       -> reenciende EC2, ajusta tu IP y verifica.
#   3. Cambios de código                -> vuelve a subir el código y reconstruye.
#
# Requisitos (en tu PC):
#   - AWS CLI v2 con credenciales válidas en ~/.aws/credentials (las del Learner Lab).
#   - Llave SSH del lab en ~/.ssh/labsuser.pem (Learner Lab -> AWS Details -> Download PEM).
#   - bash, ssh, scp, tar, curl (Git Bash en Windows sirve).
#
# Uso:
#   bash infra/aws/deploy.sh                 # despliegue completo
#   SKIP_CODE=1 bash infra/aws/deploy.sh     # solo infraestructura + verificación (sin re-subir código)
#
# Variables opcionales: PREFIX (pedidos360), AWS_REGION (us-east-1), SSH_KEY,
# KEY_NAME (vockey), INSTANCE_TYPE (t3.large), ADMIN_PASSWORD, OPERADOR_PASSWORD,
# CLIENTE_PASSWORD.
# =============================================================================
set -euo pipefail
export MSYS_NO_PATHCONV=1 AWS_PAGER=""   # Git Bash: no convertir rutas como /dev/xvda

PREFIX=${PREFIX:-pedidos360}
REGION=${AWS_REGION:-us-east-1}
export AWS_DEFAULT_REGION=$REGION
KEY_NAME=${KEY_NAME:-vockey}
SSH_KEY=${SSH_KEY:-$HOME/.ssh/labsuser.pem}
INSTANCE_TYPE=${INSTANCE_TYPE:-t3.large}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-Pedidos360Admin}
OPERADOR_PASSWORD=${OPERADOR_PASSWORD:-Pedidos360Oper}
CLIENTE_PASSWORD=${CLIENTE_PASSWORD:-Pedidos360Clie}
SKIP_CODE=${SKIP_CODE:-0}

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

AWS_BIN=$(command -v aws || true)
[ -z "$AWS_BIN" ] && [ -x "/c/Program Files/Amazon/AWSCLIV2/aws.exe" ] && AWS_BIN="/c/Program Files/Amazon/AWSCLIV2/aws.exe"
[ -z "$AWS_BIN" ] && { echo "ERROR: no se encontró AWS CLI"; exit 1; }
aws() { "$AWS_BIN" "$@"; }

SSH_OPTS=(-i "$SSH_KEY" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR -o ConnectTimeout=10)
rssh() { local host=$1; shift; ssh "${SSH_OPTS[@]}" "ec2-user@$host" "$@"; }
rscp() { scp -q "${SSH_OPTS[@]}" "$@"; }

log()  { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
ok()   { printf '    \033[32m✔\033[0m %s\n' "$*"; }
die()  { printf '\n\033[1;31mERROR: %s\033[0m\n' "$*"; exit 1; }
# aws.exe en Windows no entiende rutas /tmp de Git Bash: se convierten a C:/...
winpath() { if command -v cygpath >/dev/null; then cygpath -m "$1"; else echo "$1"; fi; }
none() { [ -z "$1" ] || [ "$1" = "None" ] || [ "$1" = "null" ]; }

# -----------------------------------------------------------------------------
log "1/9 Verificando credenciales y prerrequisitos"
ACCOUNT=$(aws sts get-caller-identity --query Account --output text 2>/dev/null) \
  || die "Credenciales AWS inválidas o vencidas. Copia las del Learner Lab (AWS Details -> AWS CLI) en ~/.aws/credentials"
ok "Cuenta $ACCOUNT, región $REGION, prefijo '$PREFIX'"
[ -f "$SSH_KEY" ] || die "No existe la llave SSH $SSH_KEY (Learner Lab -> AWS Details -> Download PEM)"
chmod 600 "$SSH_KEY" 2>/dev/null || true
MYIP=$(curl -fsS https://checkip.amazonaws.com | tr -d '[:space:]')
ok "Tu IP pública: $MYIP"

# -----------------------------------------------------------------------------
log "2/9 Red: security group"
VPC=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)
none "$VPC" && die "La cuenta no tiene VPC por defecto"
SG=$(aws ec2 describe-security-groups --filters Name=group-name,Values="$PREFIX-sg" Name=vpc-id,Values="$VPC" \
  --query 'SecurityGroups[0].GroupId' --output text)
if none "$SG"; then
  SG=$(aws ec2 create-security-group --group-name "$PREFIX-sg" --description "Pedidos360 EC2" --vpc-id "$VPC" --query GroupId --output text)
  ok "Security group creado: $SG"
else
  ok "Security group existente: $SG"
fi
allow() { aws ec2 authorize-security-group-ingress --group-id "$SG" --ip-permissions "$1" >/dev/null 2>&1 || true; }
allow "IpProtocol=-1,UserIdGroupPairs=[{GroupId=$SG}]"
allow "IpProtocol=tcp,FromPort=8080,ToPort=8080,IpRanges=[{CidrIp=0.0.0.0/0,Description=apigw-gateway}]"
allow "IpProtocol=tcp,FromPort=4200,ToPort=4200,IpRanges=[{CidrIp=0.0.0.0/0,Description=apigw-frontend}]"
for port in 22 8091; do allow "IpProtocol=tcp,FromPort=$port,ToPort=$port,IpRanges=[{CidrIp=$MYIP/32,Description=admin}]"; done
allow "IpProtocol=tcp,FromPort=15672,ToPort=15673,IpRanges=[{CidrIp=$MYIP/32,Description=admin}]"
ok "Reglas: 8080/4200 públicas (API Gateway); 22/8091/15672-15673 solo desde $MYIP"

# -----------------------------------------------------------------------------
log "3/9 Cómputo: EC2 messaging + apps (con IP elástica fija)"
cat > "$WORK/userdata.sh" <<'EOF'
#!/bin/bash
set -eux
dnf install -y docker git
systemctl enable --now docker
usermod -aG docker ec2-user
mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose
curl -fsSL https://github.com/docker/buildx/releases/download/v0.17.1/buildx-v0.17.1.linux-amd64 -o /usr/local/lib/docker/cli-plugins/docker-buildx
chmod +x /usr/local/lib/docker/cli-plugins/*
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
touch /var/tmp/bootstrap-done
EOF
AMI=$(aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 --query Parameter.Value --output text)

ensure_instance() {  # $1 = rol -> imprime instance id
  local role=$1 id state
  read -r id state < <(aws ec2 describe-instances \
    --filters Name=tag:Name,Values="$PREFIX-$role" Name=instance-state-name,Values=pending,running,stopping,stopped \
    --query 'Reservations[0].Instances[0].[InstanceId,State.Name]' --output text)
  if none "$id"; then
    id=$(aws ec2 run-instances --image-id "$AMI" --instance-type "$INSTANCE_TYPE" --key-name "$KEY_NAME" \
      --security-group-ids "$SG" --iam-instance-profile Name=LabInstanceProfile \
      --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=30,VolumeType=gp3}' \
      --user-data "file://$(winpath "$WORK/userdata.sh")" \
      --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$PREFIX-$role}]" \
      --query 'Instances[0].InstanceId' --output text)
    echo "    + EC2 $role creada: $id" >&2
  else
    [ "$state" = "stopping" ] && aws ec2 wait instance-stopped --instance-ids "$id"
    [ "$state" = "stopped" ] || [ "$state" = "stopping" ] && aws ec2 start-instances --instance-ids "$id" >/dev/null && echo "    + EC2 $role encendida: $id" >&2
    echo "    = EC2 $role existente: $id ($state)" >&2
  fi
  echo "$id"
}
ensure_eip() {  # $1 = rol, $2 = instance id -> imprime IP pública
  local role=$1 iid=$2 alloc
  alloc=$(aws ec2 describe-addresses --filters Name=tag:Name,Values="$PREFIX-$role" --query 'Addresses[0].AllocationId' --output text)
  if none "$alloc"; then
    alloc=$(aws ec2 allocate-address --domain vpc \
      --tag-specifications "ResourceType=elastic-ip,Tags=[{Key=Name,Value=$PREFIX-$role}]" --query AllocationId --output text)
  fi
  aws ec2 associate-address --instance-id "$iid" --allocation-id "$alloc" --allow-reassociation >/dev/null
  aws ec2 describe-addresses --allocation-ids "$alloc" --query 'Addresses[0].PublicIp' --output text
}
MSG_ID=$(ensure_instance messaging)
APPS_ID=$(ensure_instance apps)
aws ec2 wait instance-running --instance-ids "$MSG_ID" "$APPS_ID"
MSG_IP=$(ensure_eip messaging "$MSG_ID")
APPS_IP=$(ensure_eip apps "$APPS_ID")
MSG_PRIV=$(aws ec2 describe-instances --instance-ids "$MSG_ID" --query 'Reservations[0].Instances[0].PrivateIpAddress' --output text)
ok "messaging: $MSG_IP (privada $MSG_PRIV) | apps: $APPS_IP"

# -----------------------------------------------------------------------------
log "4/9 API Manager: Amazon API Gateway (HTTP API)"
API_ID=$(aws apigatewayv2 get-apis --query "Items[?Name=='$PREFIX-api-manager'].ApiId | [0]" --output text)
if none "$API_ID"; then
  API_ID=$(aws apigatewayv2 create-api --name "$PREFIX-api-manager" --protocol-type HTTP \
    --description "API Manager Pedidos360: valida JWT de Cognito y enruta al backend/frontend" --query ApiId --output text)
  ok "API creada: $API_ID"
fi
API_URL=$(aws apigatewayv2 get-api --api-id "$API_ID" --query ApiEndpoint --output text)
ok "URL pública: $API_URL"

# -----------------------------------------------------------------------------
log "5/9 IDaaS: Amazon Cognito (tenant, cliente OIDC, grupos y usuarios)"
POOL=$(aws cognito-idp list-user-pools --max-results 60 --query "UserPools[?Name=='$PREFIX-tenant'].Id | [0]" --output text)
if none "$POOL"; then
  POOL=$(aws cognito-idp create-user-pool --pool-name "$PREFIX-tenant" \
    --policies 'PasswordPolicy={MinimumLength=8,RequireUppercase=true,RequireLowercase=true,RequireNumbers=true,RequireSymbols=false}' \
    --schema Name=email,AttributeDataType=String,Required=true,Mutable=true Name=given_name,AttributeDataType=String,Mutable=true \
    --auto-verified-attributes email --admin-create-user-config AllowAdminCreateUserOnly=true \
    --query UserPool.Id --output text)
  ok "User Pool creado: $POOL"
else
  ok "User Pool existente: $POOL"
fi
DOMAIN=$(aws cognito-idp describe-user-pool --user-pool-id "$POOL" --query UserPool.Domain --output text)
if none "$DOMAIN"; then
  DOMAIN="$PREFIX-$ACCOUNT"
  aws cognito-idp create-user-pool-domain --domain "$DOMAIN" --user-pool-id "$POOL" >/dev/null
fi
COGNITO_DOMAIN_URL="https://$DOMAIN.auth.$REGION.amazoncognito.com"
ISSUER="https://cognito-idp.$REGION.amazonaws.com/$POOL"

CLIENT_ARGS=(--allowed-o-auth-flows code --allowed-o-auth-scopes openid email profile --allowed-o-auth-flows-user-pool-client
  --supported-identity-providers COGNITO
  --callback-urls "$API_URL/" "http://localhost:4200/" --logout-urls "$API_URL/" "http://localhost:4200/"
  --explicit-auth-flows ALLOW_REFRESH_TOKEN_AUTH ALLOW_USER_SRP_AUTH ALLOW_USER_PASSWORD_AUTH
  --access-token-validity 60 --id-token-validity 60 --token-validity-units AccessToken=minutes,IdToken=minutes
  --prevent-user-existence-errors ENABLED)
CLIENT=$(aws cognito-idp list-user-pool-clients --user-pool-id "$POOL" --max-results 60 \
  --query "UserPoolClients[?ClientName=='$PREFIX-spa'].ClientId | [0]" --output text)
if none "$CLIENT"; then
  CLIENT=$(aws cognito-idp create-user-pool-client --user-pool-id "$POOL" --client-name "$PREFIX-spa" --no-generate-secret \
    "${CLIENT_ARGS[@]}" --query UserPoolClient.ClientId --output text)
else
  aws cognito-idp update-user-pool-client --user-pool-id "$POOL" --client-id "$CLIENT" --client-name "$PREFIX-spa" \
    "${CLIENT_ARGS[@]}" >/dev/null
fi
for g in ADMIN OPERADOR CLIENTE; do aws cognito-idp create-group --user-pool-id "$POOL" --group-name "$g" >/dev/null 2>&1 || true; done
ensure_user() {  # usuario nombre grupo clave
  aws cognito-idp admin-create-user --user-pool-id "$POOL" --username "$1" --message-action SUPPRESS \
    --user-attributes Name=email,Value="$1@pedidos360.cl" Name=email_verified,Value=true Name=given_name,Value="$2" >/dev/null 2>&1 || true
  aws cognito-idp admin-set-user-password --user-pool-id "$POOL" --username "$1" --password "$4" --permanent
  aws cognito-idp admin-add-user-to-group --user-pool-id "$POOL" --username "$1" --group-name "$3"
}
ensure_user admin Administrador ADMIN "$ADMIN_PASSWORD"
ensure_user operador Operador OPERADOR "$OPERADOR_PASSWORD"
ensure_user cliente Cliente CLIENTE "$CLIENTE_PASSWORD"
ok "Pool $POOL | client $CLIENT | Hosted UI $COGNITO_DOMAIN_URL | usuarios admin/operador/cliente"

# -----------------------------------------------------------------------------
log "6/9 API Gateway: JWT authorizer, integraciones y rutas"
AUTH_ID=$(aws apigatewayv2 get-authorizers --api-id "$API_ID" --query "Items[?Name=='cognito-jwt'].AuthorizerId | [0]" --output text)
JWT_CFG="Audience=$CLIENT,Issuer=$ISSUER"
if none "$AUTH_ID"; then
  AUTH_ID=$(aws apigatewayv2 create-authorizer --api-id "$API_ID" --name cognito-jwt --authorizer-type JWT \
    --identity-source '$request.header.Authorization' --jwt-configuration "$JWT_CFG" --query AuthorizerId --output text)
else
  aws apigatewayv2 update-authorizer --api-id "$API_ID" --authorizer-id "$AUTH_ID" --jwt-configuration "$JWT_CFG" >/dev/null
fi

ensure_integration() {  # $1 = sufijo que identifica la integración, $2 = URI destino, $3 = método
  local suffix=$1 uri=$2 method=$3 iid
  iid=$(aws apigatewayv2 get-integrations --api-id "$API_ID" \
    --query "Items[?ends_with(IntegrationUri, '$suffix')].IntegrationId | [0]" --output text)
  if none "$iid"; then
    iid=$(aws apigatewayv2 create-integration --api-id "$API_ID" --integration-type HTTP_PROXY --integration-method "$method" \
      --integration-uri "$uri" --payload-format-version 1.0 --query IntegrationId --output text)
  else
    aws apigatewayv2 update-integration --api-id "$API_ID" --integration-id "$iid" --integration-uri "$uri" >/dev/null
  fi
  echo "$iid"
}
I_API=$(ensure_integration ':8080/api/{proxy}' "http://$APPS_IP:8080/api/{proxy}" ANY)
I_WEB=$(ensure_integration ':4200/{proxy}' "http://$APPS_IP:4200/{proxy}" GET)
I_ROOT=$(ensure_integration ':4200/' "http://$APPS_IP:4200/" GET)

ensure_route() {  # $1 = route key, $2 = integration id, $3 = jwt|none
  local key=$1 iid=$2 auth=$3 rid
  local auth_args=(--authorization-type NONE)
  [ "$auth" = jwt ] && auth_args=(--authorization-type JWT --authorizer-id "$AUTH_ID")
  rid=$(aws apigatewayv2 get-routes --api-id "$API_ID" --query "Items[?RouteKey=='$key'].RouteId | [0]" --output text)
  if none "$rid"; then
    aws apigatewayv2 create-route --api-id "$API_ID" --route-key "$key" --target "integrations/$iid" "${auth_args[@]}" >/dev/null
  else
    aws apigatewayv2 update-route --api-id "$API_ID" --route-id "$rid" --target "integrations/$iid" "${auth_args[@]}" >/dev/null
  fi
}
ensure_route 'ANY /api/{proxy+}' "$I_API" jwt
ensure_route 'GET /{proxy+}' "$I_WEB" none
ensure_route 'GET /' "$I_ROOT" none
aws apigatewayv2 get-stage --api-id "$API_ID" --stage-name '$default' >/dev/null 2>&1 \
  || aws apigatewayv2 create-stage --api-id "$API_ID" --stage-name '$default' --auto-deploy >/dev/null
ok "ANY /api/{proxy+} [JWT] -> $APPS_IP:8080 | GET / y /{proxy+} -> $APPS_IP:4200"

# -----------------------------------------------------------------------------
log "7/9 Esperando que las EC2 terminen su bootstrap (Docker)"
for host in "$MSG_IP" "$APPS_IP"; do
  for i in $(seq 1 60); do
    rssh "$host" 'test -f /var/tmp/bootstrap-done' 2>/dev/null && break
    [ "$i" = 60 ] && die "La EC2 $host no terminó su bootstrap"
    sleep 10
  done
  ok "$host listo"
done

# -----------------------------------------------------------------------------
if [ "$SKIP_CODE" != "1" ]; then
  log "8/9 Subiendo código y levantando contenedores (el primer build tarda ~10 min)"
  # Conserva los secretos existentes (el volumen de PostgreSQL ya usa esa clave).
  # Se busca en ambos hosts por si uno perdió su copia.
  rssh "$APPS_IP" 'cat ~/pedidos360/infra/aws/.env 2>/dev/null' > "$WORK/old.env" || true
  [ -s "$WORK/old.env" ] || rssh "$MSG_IP" 'cat ~/pedidos360/infra/aws/.env 2>/dev/null' > "$WORK/old.env" || true
  old() { grep -E "^$1=" "$WORK/old.env" 2>/dev/null | head -1 | cut -d= -f2- || true; }
  rnd() { head -c 32 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 20; }
  DB_PASSWORD=$(old DB_PASSWORD); [ -z "$DB_PASSWORD" ] && DB_PASSWORD=$(rnd)
  RABBIT_PASSWORD=$(old RABBITMQ_PASSWORD); [ -z "$RABBIT_PASSWORD" ] && RABBIT_PASSWORD=$(rnd)
  RABBIT_COOKIE=$(old RABBITMQ_ERLANG_COOKIE); [ -z "$RABBIT_COOKIE" ] && RABBIT_COOKIE=$(rnd)
  cat > "$WORK/.env" <<EOF
MESSAGING_HOST=$MSG_PRIV
KAFKA_BOOTSTRAP_SERVERS=$MSG_PRIV:9092,$MSG_PRIV:9093,$MSG_PRIV:9094
DB_NAME=pedidos360
DB_USERNAME=postgres
DB_PASSWORD=$DB_PASSWORD
RABBITMQ_USERNAME=pedidos360
RABBITMQ_PASSWORD=$RABBIT_PASSWORD
RABBITMQ_ERLANG_COOKIE=$RABBIT_COOKIE
JWT_ISSUER_URI=$ISSUER
JWT_AUDIENCE=$CLIENT
COGNITO_DOMAIN_URL=$COGNITO_DOMAIN_URL
EOF
  (cd "$ROOT" && tar --exclude=node_modules --exclude=target --exclude=dist --exclude=.git --exclude=.omo \
     --exclude=.angular --exclude='*.env' --exclude=.env --warning=no-file-changed -czf "$WORK/code.tgz" .) \
    || [ $? -eq 1 ]   # 1 = algún archivo cambió mientras se empaquetaba (no es fatal)
  for host in "$MSG_IP" "$APPS_IP"; do
    rscp "$WORK/code.tgz" "$WORK/.env" "ec2-user@$host:/tmp/"
    # sudo: builds/tests en contenedores pueden dejar archivos de root (target/).
    rssh "$host" 'set -e
                  sudo rm -rf ~/pedidos360 && mkdir -p ~/pedidos360
                  tar -xzf /tmp/code.tgz -C ~/pedidos360 --warning=no-unknown-keyword
                  find ~/pedidos360 -name "*.sh" -exec sed -i "s/\r$//" {} +
                  install -m 600 /tmp/.env ~/pedidos360/infra/aws/.env
                  rm -f /tmp/.env /tmp/code.tgz'
  done
  ok "Código y .env distribuidos"

  rssh "$MSG_IP" 'cd ~/pedidos360/infra/aws && docker compose -f docker-compose.messaging.yml --env-file .env up -d --remove-orphans >/tmp/up.log 2>&1 || { tail -20 /tmp/up.log; exit 1; }'
  for i in $(seq 1 60); do
    st=$(rssh "$MSG_IP" 'docker inspect -f "{{.State.Status}} {{.State.ExitCode}}" aws-kafka-init-1 2>/dev/null' || true)
    [ "$st" = "exited 0" ] && break
    [ "${st%% *}" = "exited" ] && die "kafka-init falló: $(rssh "$MSG_IP" 'docker logs --tail 10 aws-kafka-init-1')"
    sleep 5
  done
  ok "Kafka (3 ZK + 3 brokers), tópicos 3x3, Kafka UI y RabbitMQ (2 nodos) arriba"

  rssh "$APPS_IP" 'cd ~/pedidos360/infra/aws && docker compose -f docker-compose.apps.yml --env-file .env up -d --build --remove-orphans >/tmp/up.log 2>&1 || { tail -30 /tmp/up.log; exit 1; }'
  ok "Microservicios y frontend construidos y arriba"
else
  log "8/9 SKIP_CODE=1: se omite subir código (los contenedores se reinician solos con la EC2)"
fi

# -----------------------------------------------------------------------------
log "9/9 Verificación de punta a punta (vía API Manager)"
code() { curl -s -o /dev/null -w '%{http_code}' --max-time 15 "$@"; }
TOKEN=$(aws cognito-idp initiate-auth --auth-flow USER_PASSWORD_AUTH --client-id "$CLIENT" \
  --auth-parameters USERNAME=admin,PASSWORD="$ADMIN_PASSWORD" --query AuthenticationResult.AccessToken --output text)
for i in $(seq 1 40); do
  [ "$(code -H "Authorization: Bearer $TOKEN" "$API_URL/api/pedidos")" = 200 ] && break
  [ "$i" = 40 ] && die "El backend no respondió 200 a tiempo (revisa: docker compose -f docker-compose.apps.yml logs)"
  sleep 10
done
ok "Frontend:            $(code "$API_URL/")"
ok "config.json:         $(curl -s "$API_URL/config.json" | grep -o '"clientId": "[^"]*"')"
ok "API sin token:       $(code "$API_URL/api/pedidos")  (esperado 401)"
ok "API token inválido:  $(code -H 'Authorization: Bearer x.y.z' "$API_URL/api/pedidos")  (esperado 401)"
ok "API token válido:    $(code -H "Authorization: Bearer $TOKEN" "$API_URL/api/pedidos")  (esperado 200)"

cat <<EOF

=====================================================================
 Pedidos360 desplegado en la cuenta $ACCOUNT ($REGION)
---------------------------------------------------------------------
 App (API Manager):  $API_URL
 Usuarios:           admin / $ADMIN_PASSWORD   operador / $OPERADOR_PASSWORD   cliente / $CLIENTE_PASSWORD
 Kafka UI:           http://$MSG_IP:8091
 RabbitMQ UI:        http://$MSG_IP:15672   (nodo 2: :15673)
 SSH:                ssh -i $SSH_KEY ec2-user@$MSG_IP   |   ec2-user@$APPS_IP
 Cognito:            pool $POOL  client $CLIENT
=====================================================================
EOF
