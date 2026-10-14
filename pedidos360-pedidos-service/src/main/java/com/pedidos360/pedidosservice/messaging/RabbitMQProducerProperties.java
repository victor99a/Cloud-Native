package com.pedidos360.pedidosservice.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges y routing keys usados por el productor de eventos,
 * centralizados en {@code application.yml} (prefijo {@code pedidos360.rabbitmq}).
 */
@ConfigurationProperties(prefix = "pedidos360.rabbitmq")
public record RabbitMQProducerProperties(
        String exchangeTopic,
        String exchangeDirect,
        String routingPedidoCreado,
        String routingPedidoEstadoPrefix
) {
}
