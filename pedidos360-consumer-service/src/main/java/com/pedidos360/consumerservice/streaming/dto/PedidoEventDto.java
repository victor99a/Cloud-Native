package com.pedidos360.consumerservice.streaming.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento de pedido leído desde el tópico {@code pedidos360.pedidos.eventos}.
 * Depende solo de la forma JSON publicada por pedidos-service (desacoplado).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoEventDto(
        Long pedidoId,
        String numero,
        String cliente,
        String estado,
        BigDecimal total,
        LocalDateTime fecha
) {
}
