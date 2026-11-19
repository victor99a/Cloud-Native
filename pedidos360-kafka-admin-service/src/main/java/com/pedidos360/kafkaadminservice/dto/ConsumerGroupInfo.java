package com.pedidos360.kafkaadminservice.dto;

import java.util.List;

/**
 * Información de un consumer group (respuesta de descripción).
 */
public record ConsumerGroupInfo(
        String groupId,
        boolean isSimpleConsumerGroup,
        String state,
        List<MemberInfo> members
) {
    public record MemberInfo(
            String consumerId,
            String clientId,
            String host,
            List<String> assignedPartitions
    ) {
    }
}
