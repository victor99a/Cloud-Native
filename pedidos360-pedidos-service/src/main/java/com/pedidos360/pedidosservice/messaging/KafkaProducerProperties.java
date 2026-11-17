package com.pedidos360.pedidosservice.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de tópicos Kafka usados por el productor de eventos, centralizados en
 * {@code application.yml} (prefijo {@code pedidos360.kafka}).
 */
@ConfigurationProperties(prefix = "pedidos360.kafka")
public record KafkaProducerProperties(
        String topicPedidosEventos
) {
}
