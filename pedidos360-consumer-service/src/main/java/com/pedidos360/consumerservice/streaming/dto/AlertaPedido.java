package com.pedidos360.consumerservice.streaming.dto;

/**
 * Alerta operacional derivada de un evento de pedido.
 */
public record AlertaPedido(
        TipoAlerta tipo,
        String numeroPedido,
        String mensaje
) {

    public enum TipoAlerta {
        /** Pedido cuyo total supera el umbral configurado. */
        ALTO_VALOR,
        /** Pedido cancelado: requiere revisión (stock, devolución). */
        CANCELADO,
        /** Pedido entregado: cierre del ciclo, encuesta de satisfacción. */
        ENTREGADO
    }
}
