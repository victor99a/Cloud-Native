package com.pedidos360.bff.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PedidoRequest(
        @NotBlank(message = "El cliente es obligatorio")
        String cliente,

        @NotEmpty(message = "El pedido debe tener al menos un item")
        @Valid
        List<PedidoItemRequest> items
) {
    public record PedidoItemRequest(
            @NotNull(message = "El producto es obligatorio")
            Long productoId,

            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor a 0")
            Integer cantidad
    ) {
    }
}
