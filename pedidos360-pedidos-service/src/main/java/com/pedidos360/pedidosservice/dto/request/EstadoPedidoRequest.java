package com.pedidos360.pedidosservice.dto.request;

import com.pedidos360.pedidosservice.entity.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record EstadoPedidoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPedido estado
) {
}
