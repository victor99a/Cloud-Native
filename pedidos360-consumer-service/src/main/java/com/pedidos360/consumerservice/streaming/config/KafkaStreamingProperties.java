package com.pedidos360.consumerservice.streaming.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades Kafka del consumidor de alertas (prefijo {@code pedidos360.kafka}).
 */
@ConfigurationProperties(prefix = "pedidos360.kafka")
public record KafkaStreamingProperties(
        String topicPedidosEventos,
        String consumerGroupAlertas,
        BigDecimal umbralAltoValor
) {
}
