package com.pedidos360.kafkaadminservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Petición para aumentar el número de particiones de un tópico (Kafka no permite
 * reducirlas). Útil para escalar el paralelismo de los consumer groups.
 */
public record PartitionsRequest(
        @NotNull(message = "totalPartitions es obligatorio")
        @Min(value = 1, message = "totalPartitions debe ser >= 1")
        @Max(value = 100, message = "totalPartitions debe ser <= 100")
        Integer totalPartitions
) {
}
