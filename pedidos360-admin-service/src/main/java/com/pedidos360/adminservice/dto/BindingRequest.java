package com.pedidos360.adminservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record BindingRequest(
        @NotBlank(message = "La cola del binding es obligatoria")
        @Size(max = 255)
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$",
                message = "El nombre de la cola solo admite letras, números, '.', '_' y '-'")
        String queue,

        @NotBlank(message = "El exchange del binding es obligatorio")
        @Size(max = 255)
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$",
                message = "El nombre del exchange solo admite letras, números, '.', '_' y '-'")
        String exchange,

        @NotBlank(message = "La routing key del binding es obligatoria")
        @Size(max = 255)
        @Pattern(regexp = "^[A-Za-z0-9._*#-]+$",
                message = "La routing key solo admite letras, números, '.', '_', '*', '#' y '-'")
        String routingKey,

        Map<String, Object> arguments
) {
}
