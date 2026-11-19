package com.pedidos360.consumerservice.streaming.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento de pedido leído desde el tópico {@code pedidos360.pedidos.eventos}.
 * Depende solo de la forma JSON publicada por pedidos-service (desacoplado).
 * {@code tipoEvento} puede ser nulo en eventos anteriores a la versión 1.0.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoEventDto(
        String eventId,
        String tipoEvento,
        Long pedidoId,
        String numero,
        String cliente,
        String estado,
        BigDecimal total,
        LocalDateTime fecha
) {

    /** Alta de pedido (o evento legado sin tipo, que siempre era una alta o un cambio). */
    public boolean esCreacion() {
        return tipoEvento == null || "PEDIDO_CREADO".equals(tipoEvento);
    }

    public boolean esEliminacion() {
        return "PEDIDO_ELIMINADO".equals(tipoEvento);
    }
}
