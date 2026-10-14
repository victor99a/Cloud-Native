package com.pedidos360.adminservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record BindingRequest(
        @NotBlank(message = "La cola del binding es obligatoria")
        @Size(max = 255)
        String queue,

        @NotBlank(message = "El exchange del binding es obligatorio")
        @Size(max = 255)
        String exchange,

        @NotBlank(message = "La routing key del binding es obligatoria")
        @Size(max = 255)
        String routingKey,

        Map<String, Object> arguments
) {
}
