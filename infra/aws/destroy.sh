#!/usr/bin/env bash
# =============================================================================
# Elimina TODA la infraestructura de Pedidos360 creada por deploy.sh en la cuenta
# actual (EC2, IPs elásticas, security group, API Gateway y Cognito).
#
# Uso:  bash infra/aws/destroy.sh            (pide confirmación)
#       PREFIX=otro bash infra/aws/destroy.sh
# =============================================================================
set -euo pipefail
export MSYS_NO_PATHCONV=1 AWS_PAGER=""
PREFIX=${PREFIX:-pedidos360}
export AWS_DEFAULT_REGION=${AWS_REGION:-us-east-1}

AWS_BIN=$(command -v aws || true)
[ -z "$AWS_BIN" ] && [ -x "/c/Program Files/Amazon/AWSCLIV2/aws.exe" ] && AWS_BIN="/c/Program Files/Amazon/AWSCLIV2/aws.exe"
aws() { "$AWS_BIN" "$@"; }
none() { [ -z "$1" ] || [ "$1" = "None" ] || [ "$1" = "null" ]; }

ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
if [ "${YES:-0}" != "1" ]; then
  read -r -p "Se eliminará TODO lo de '$PREFIX' en la cuenta $ACCOUNT. ¿Continuar? (si/no) " answer
  [ "$answer" = "si" ] || { echo "Cancelado."; exit 0; }
fi

echo "==> API Gateway"
API_ID=$(aws apigatewayv2 get-apis --query "Items[?Name=='$PREFIX-api-manager'].ApiId | [0]" --output text)
none "$API_ID" || { aws apigatewayv2 delete-api --api-id "$API_ID"; echo "    - API $API_ID"; }

echo "==> Cognito"
POOL=$(aws cognito-idp list-user-pools --max-results 60 --query "UserPools[?Name=='$PREFIX-tenant'].Id | [0]" --output text)
if ! none "$POOL"; then
  DOMAIN=$(aws cognito-idp describe-user-pool --user-pool-id "$POOL" --query UserPool.Domain --output text)
  none "$DOMAIN" || aws cognito-idp delete-user-pool-domain --domain "$DOMAIN" --user-pool-id "$POOL"
  aws cognito-idp delete-user-pool --user-pool-id "$POOL"
  echo "    - User Pool $POOL"
fi

echo "==> EC2 e IPs elásticas"
IDS=$(aws ec2 describe-instances --filters "Name=tag:Name,Values=$PREFIX-messaging,$PREFIX-apps" \
  Name=instance-state-name,Values=pending,running,stopping,stopped --query 'Reservations[].Instances[].InstanceId' --output text)
for role in messaging apps; do
  read -r alloc assoc < <(aws ec2 describe-addresses --filters Name=tag:Name,Values="$PREFIX-$role" \
    --query 'Addresses[0].[AllocationId,AssociationId]' --output text)
  none "$assoc" || aws ec2 disassociate-address --association-id "$assoc"
  none "$alloc" || { aws ec2 release-address --allocation-id "$alloc"; echo "    - EIP $role"; }
done
if [ -n "$IDS" ]; then
  aws ec2 terminate-instances --instance-ids $IDS >/dev/null
  echo "    - terminando $IDS (esperando...)"
  aws ec2 wait instance-terminated --instance-ids $IDS
fi

echo "==> Security group"
SG=$(aws ec2 describe-security-groups --filters Name=group-name,Values="$PREFIX-sg" --query 'SecurityGroups[0].GroupId' --output text)
none "$SG" || { aws ec2 delete-security-group --group-id "$SG"; echo "    - $SG"; }

echo "Listo: infraestructura '$PREFIX' eliminada."
