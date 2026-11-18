package com.pedidos360.kafkaadminservice.dto;

import java.util.List;

/**
 * Estado del clúster Kafka: id, broker controlador y brokers activos.
 */
public record ClusterInfo(
        String clusterId,
        Integer controllerId,
        List<BrokerInfo> brokers
) {
    public record BrokerInfo(int id, String host, int port, String rack) {
    }
}
