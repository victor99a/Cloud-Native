package com.pedidos360.analyticsservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Métrica agregada de pedidos publicada en el tópico {@code pedidos360.analitica.metricas}.
 * La clave del mensaje es el {@code estado}, de modo que todas las métricas de un
 * mismo estado caen en la misma partición (orden y compactación coherentes).
 */
public record MetricasPedidoDto(
        String estado,
        long totalPedidos,
        BigDecimal totalVendido,
        LocalDateTime ultimaActualizacion
) {
}
