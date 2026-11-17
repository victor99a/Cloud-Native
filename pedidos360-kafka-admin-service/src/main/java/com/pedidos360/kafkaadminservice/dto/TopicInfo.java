package com.pedidos360.kafkaadminservice.dto;

import java.util.List;
import java.util.Map;

/**
 * Información de un tópico (respuesta de listado/descripción).
 */
public record TopicInfo(
        String name,
        int partitions,
        int replicas,
        Map<String, String> configs,
        List<PartitionInfo> partitionInfos
) {
    public record PartitionInfo(
            int partition,
            Integer leader,
            List<Integer> replicas,
            List<Integer> isr
    ) {
    }
}
