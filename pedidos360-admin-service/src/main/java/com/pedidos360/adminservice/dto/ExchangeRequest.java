package com.pedidos360.adminservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExchangeRequest(
        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Size(max = 255, message = "El nombre del exchange no puede superar 255 caracteres")
        String name,

        @NotBlank(message = "El tipo de exchange es obligatorio (direct, topic, fanout, headers)")
        String type,

        Boolean durable,
        Boolean autoDelete
) {
}
