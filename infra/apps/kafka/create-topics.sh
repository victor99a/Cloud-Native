#!/bin/bash
# =============================================================================
# Inicializador de tópicos de Kafka (one-shot, corre dentro del clúster).
#
# Crea EXACTAMENTE 2 tópicos de datos, cada uno con 3 particiones y 3 réplicas,
# y ajusta los parámetros de administración pedidos:
#   - pedidos360.pedidos.eventos   : flujo de eventos de dominio (retention 7 días,
#                                    cleanup.policy=delete).
#   - pedidos360.analitica.metricas: flujo de métricas agregadas (retention 30 días,
#                                    cleanup.policy=compact).
# También crea el tópico AUXILIAR de resiliencia (Dead Letter) para los mensajes
# defectuosos.
#
# Cada --create se REINTENTA: --replication-factor 3 exige que los 3 brokers ya
# estén registrados en el clúster; si un broker aún no se registró, el comando
# falla y se reintenta en lugar de abortar.
# =============================================================================
set -e

BOOTSTRAP=${BOOTSTRAP_SERVERS:-kafka1:29092}
RETRIES=60

echo ">> Esperando a que el clúster Kafka esté listo (${BOOTSTRAP})..."
for i in $(seq 1 "$RETRIES"); do
  if kafka-topics --bootstrap-server "$BOOTSTRAP" --list >/dev/null 2>&1; then
    echo ">> Brokers listos tras ${i} intento(s)."
    break
  fi
  echo "   ... esperando brokers (${i}/${RETRIES})"
  sleep 2
done

kafka-topics --bootstrap-server "$BOOTSTRAP" --list >/dev/null 2>&1 || {
  echo ">> ERROR: no se pudo conectar a Kafka en ${BOOTSTRAP}."
  exit 1
}

# Crea un tópico reintentando hasta que todos los brokers estén registrados.
# Uso: create_topic <nombre> <particiones> <replicas> [--config k=v ...]
create_topic() {
  local name="$1"
  local partitions="$2"
  local replication="$3"
  shift 3

  for attempt in $(seq 1 "$RETRIES"); do
    if kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
        --topic "$name" --partitions "$partitions" --replication-factor "$replication" \
        "$@" >/dev/null 2>&1; then
      echo ">> Tópico '${name}' listo (intento ${attempt})."
      return 0
    fi
    echo "   ... reintentando creación de '${name}' (${attempt}/${RETRIES})"
    sleep 2
  done

  echo ">> ERROR: no se pudo crear el tópico '${name}'."
  exit 1
}

echo ">> Creando tópico 'pedidos360.pedidos.eventos' (3 particiones, 3 réplicas, retention 7d)..."
create_topic pedidos360.pedidos.eventos 3 3 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete

echo ">> Creando tópico 'pedidos360.analitica.metricas' (3 particiones, 3 réplicas, retention 30d, compact)..."
create_topic pedidos360.analitica.metricas 3 3 \
  --config retention.ms=2592000000 \
  --config cleanup.policy=compact

# Tópico auxiliar de resiliencia (Dead Letter): los mensajes defectuosos se
# desvían aquí tras agotar los reintentos. No es un tópico de flujo de datos.
echo ">> Creando tópico auxiliar 'pedidos360.pedidos.eventos.dlt' (3 particiones, 3 réplicas)..."
create_topic pedidos360.pedidos.eventos.dlt 3 3 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete

echo ">> Tópicos existentes en el clúster:"
kafka-topics --bootstrap-server "$BOOTSTRAP" --list

echo ">> Inicialización completada."
