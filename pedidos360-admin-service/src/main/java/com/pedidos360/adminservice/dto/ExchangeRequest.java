package com.pedidos360.adminservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExchangeRequest(
        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Size(max = 255, message = "El nombre del exchange no puede superar 255 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$",
                message = "El nombre solo admite letras, números, '.', '_' y '-'")
        String name,

        @NotBlank(message = "El tipo de exchange es obligatorio (direct, topic, fanout, headers)")
        @Pattern(regexp = "(?i)^(direct|topic|fanout|headers)$",
                message = "El tipo de exchange debe ser direct, topic, fanout o headers")
        String type,

        Boolean durable,
        Boolean autoDelete
) {
}
