package com.pedidos360.productosservice.dto.response;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String sku,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        Boolean activo
) {
}
