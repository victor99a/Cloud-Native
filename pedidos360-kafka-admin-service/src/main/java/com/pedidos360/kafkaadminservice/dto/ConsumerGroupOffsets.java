package com.pedidos360.kafkaadminservice.dto;

import java.util.List;

/**
 * Offsets y lag de un consumer group por partición.
 *
 * <p>{@code lag = endOffset - committedOffset}: cuántos mensajes le faltan al
 * grupo por procesar. Un lag que crece de forma sostenida anticipa un problema
 * operacional (consumidor caído, lento o con errores).
 */
public record ConsumerGroupOffsets(
        String groupId,
        long totalLag,
        List<PartitionOffset> partitions
) {
    public record PartitionOffset(
            String topic,
            int partition,
            long committedOffset,
            long endOffset,
            long lag
    ) {
    }
}
