package com.pedidos360.kafkaadminservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

/**
 * Petición para crear un tópico de Kafka.
 *
 * <p>Si no se indica {@code partitions} ni {@code replicas}, el servicio usa
 * {@code 3} y {@code 3} respectivamente (tolerancia a fallos + paralelismo).
 */
public record TopicRequest(
        @NotBlank(message = "El nombre del tópico es obligatorio")
        @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "Nombre de tópico inválido")
        String name,

        @Min(value = 1, message = "El número de particiones debe ser >= 1")
        @Max(value = 100, message = "El número de particiones debe ser <= 100")
        Integer partitions,

        @Min(value = 1, message = "El factor de replicación debe ser >= 1")
        Integer replicas,

        Map<String, String> configs
) {
}
