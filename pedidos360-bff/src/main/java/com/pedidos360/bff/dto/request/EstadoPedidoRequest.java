package com.pedidos360.bff.dto.request;

import com.pedidos360.bff.entity.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record EstadoPedidoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPedido estado
) {
}
