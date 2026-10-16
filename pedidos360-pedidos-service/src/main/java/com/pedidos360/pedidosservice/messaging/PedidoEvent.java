package com.pedidos360.pedidosservice.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento de dominio publicado a RabbitMQ cuando se crea o cambia de estado un pedido.
 */
public record PedidoEvent(
        Long pedidoId,
        String numero,
        String cliente,
        String estado,
        BigDecimal total,
        LocalDateTime fecha
) {
}
