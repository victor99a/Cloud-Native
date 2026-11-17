package com.pedidos360.bff.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Métrica de analítica de pedidos devuelta al frontend (espejo del DTO del
 * analytics-service).
 */
public record MetricaAnaliticaResponse(
        String estado,
        long totalPedidos,
        BigDecimal totalVendido,
        LocalDateTime ultimaActualizacion
) {
}
