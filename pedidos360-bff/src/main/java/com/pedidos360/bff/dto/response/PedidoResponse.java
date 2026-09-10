package com.pedidos360.bff.dto.response;

import com.pedidos360.bff.entity.EstadoPedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        String numero,
        String cliente,
        LocalDateTime fecha,
        EstadoPedido estado,
        BigDecimal total,
        List<ItemResponse> items
) {
    public record ItemResponse(
            Long productoId,
            String nombreProducto,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {
    }
}
