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
 *
 * <p>{@code eventId} permite descartar duplicados y {@code tipoEvento}
 * ({@code PEDIDO_CREADO}, {@code ESTADO_ACTUALIZADO}, {@code PEDIDO_ELIMINADO})
 * decide cómo se ajustan las métricas. Ambos pueden venir nulos en eventos
 * antiguos (anteriores a la versión 1.0); en ese caso se tratan como alta/cambio.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoEventDto(
        String eventId,
        String tipoEvento,
        LocalDateTime ocurridoEn,
        Long pedidoId,
        String numero,
        String cliente,
        String estado,
        BigDecimal total,
        LocalDateTime fecha
) {

    public static final String PEDIDO_ELIMINADO = "PEDIDO_ELIMINADO";

    public boolean esEliminacion() {
        return PEDIDO_ELIMINADO.equals(tipoEvento);
    }
}
