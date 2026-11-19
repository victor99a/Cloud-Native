package com.pedidos360.pedidosservice.messaging;

/** Tipo de hecho de negocio que representa un {@link PedidoEvent}. */
public enum TipoEventoPedido {
    PEDIDO_CREADO,
    ESTADO_ACTUALIZADO,
    PEDIDO_ELIMINADO
}
