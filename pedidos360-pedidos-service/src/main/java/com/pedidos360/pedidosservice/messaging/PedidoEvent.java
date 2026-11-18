package com.pedidos360.pedidosservice.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento de dominio publicado a RabbitMQ (exchanges topic/direct) y a Kafka
 * (tópico {@code pedidos360.pedidos.eventos}) cuando se crea o cambia de estado un pedido.
 * Cada campo cubre un caso de uso: {@code pedidoId}/{@code numero} identifican y
 * particionan (clave), {@code estado} agrupa métricas y dispara alertas,
 * {@code total} alimenta el monto vendido y el umbral de alto valor,
 * {@code cliente} personaliza notificaciones y {@code fecha} ordena en el tiempo.
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
