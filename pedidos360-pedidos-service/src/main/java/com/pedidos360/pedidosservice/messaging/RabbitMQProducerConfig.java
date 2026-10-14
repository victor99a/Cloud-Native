package com.pedidos360.pedidosservice.messaging;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita el binding de las propiedades centralizadas de RabbitMQ del productor.
 */
@Configuration
@EnableConfigurationProperties(RabbitMQProducerProperties.class)
public class RabbitMQProducerConfig {
}
