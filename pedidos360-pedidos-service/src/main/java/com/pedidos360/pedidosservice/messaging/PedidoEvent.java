package com.pedidos360.pedidosservice.messaging;

import com.pedidos360.pedidosservice.dto.response.PedidoResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de dominio publicado a RabbitMQ (exchanges topic/direct) y a Kafka
 * (tópico {@code pedidos360.pedidos.eventos}) cuando se crea, cambia de estado o
 * elimina un pedido.
 *
 * <p>Cada campo cubre un caso de uso:
 * <ul>
 *   <li>{@code eventId}: identificador único del evento; permite a los consumidores
 *       descartar duplicados (idempotencia ante reintentos o rebalanceos).</li>
 *   <li>{@code tipoEvento}: qué ocurrió ({@link TipoEventoPedido}); los consumidores
 *       reaccionan distinto ante una creación, un cambio de estado o una eliminación.</li>
 *   <li>{@code ocurridoEn}: instante del hecho de negocio (trazabilidad).</li>
 *   <li>{@code pedidoId}/{@code numero}: identifican y particionan (clave Kafka).</li>
 *   <li>{@code estado}: agrupa métricas y dispara alertas.</li>
 *   <li>{@code total}: alimenta el monto vendido y el umbral de alto valor.</li>
 *   <li>{@code cliente}: personaliza notificaciones; {@code fecha}: fecha del pedido.</li>
 * </ul>
 */
public record PedidoEvent(
        String eventId,
        TipoEventoPedido tipoEvento,
        LocalDateTime ocurridoEn,
        Long pedidoId,
        String numero,
        String cliente,
        String estado,
        BigDecimal total,
        LocalDateTime fecha
) {

    /** Construye un evento nuevo (eventId único) a partir del estado actual del pedido. */
    public static PedidoEvent of(TipoEventoPedido tipo, PedidoResponse pedido) {
        return new PedidoEvent(
                UUID.randomUUID().toString(),
                tipo,
                LocalDateTime.now(),
                pedido.id(),
                pedido.numero(),
                pedido.cliente(),
                pedido.estado().name(),
                pedido.total(),
                pedido.fecha()
        );
    }
}
