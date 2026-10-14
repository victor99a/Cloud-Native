package com.pedidos360.consumerservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de colas, exchanges y routing keys de RabbitMQ, centralizados en
 * {@code application.yml} (prefijo {@code pedidos360.rabbitmq}). Cero hardcoding:
 * ningún servicio o controlador escribe literales de nombres de colas/exchanges.
 */
@ConfigurationProperties(prefix = "pedidos360.rabbitmq")
public record RabbitMQProperties(
        String exchangeTopic,
        String exchangeDirect,
        String exchangeDlx,
        String queueNotificaciones,
        String queueTickets,
        String queueDocumentos,
        String queueNotificacionesDlq,
        String queueTicketsDlq,
        String queueDocumentosDlq,
        String routingPedidoCreado,
        String routingPedidoTodos,
        String routingPedidoEntregado
) {
}
