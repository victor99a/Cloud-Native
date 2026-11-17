package com.pedidos360.analyticsservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento de dominio de pedido consumido desde el tópico {@code pedidos360.pedidos.eventos}.
 *
 * <p>Es un espejo tipado del {@code PedidoEvent} publicado por el
 * {@code pedidos-service}; solo depende de la forma JSON (no de la clase Java del
 * productor), de modo que ambos servicios permanecen desacoplados.
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
