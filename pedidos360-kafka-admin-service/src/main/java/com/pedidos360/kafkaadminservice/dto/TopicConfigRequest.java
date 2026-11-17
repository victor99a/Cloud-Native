package com.pedidos360.kafkaadminservice.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/**
 * Petición para modificar la configuración de un tópico (retention.ms,
 * cleanup.policy, min.insync.replicas, etc.).
 */
public record TopicConfigRequest(
        @NotEmpty(message = "Debe indicar al menos una configuración")
        Map<String, String> configs
) {
}
