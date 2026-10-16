package com.pedidos360.adminservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record QueueRequest(
        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Size(max = 255, message = "El nombre de la cola no puede superar 255 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$",
                message = "El nombre solo admite letras, números, '.', '_' y '-'")
        String name,

        Boolean durable,
        Boolean exclusive,
        Boolean autoDelete,
        Map<String, Object> arguments
) {
}
