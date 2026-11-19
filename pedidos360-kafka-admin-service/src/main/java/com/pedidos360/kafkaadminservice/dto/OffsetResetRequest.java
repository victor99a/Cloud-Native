package com.pedidos360.kafkaadminservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Petición para reposicionar los offsets de un consumer group sobre un tópico
 * (reprocesar desde el inicio o saltar al final). El grupo debe estar inactivo.
 */
public record OffsetResetRequest(
        @NotBlank(message = "El tópico es obligatorio")
        String topic,

        @NotBlank(message = "El destino es obligatorio")
        @Pattern(regexp = "earliest|latest", message = "El destino debe ser 'earliest' o 'latest'")
        String to
) {
}
