package com.pedidos360.adminservice.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la infraestructura de administración de RabbitMQ.
 * Expone un {@link AmqpAdmin} para que el {@code RabbitAdminService} manipule
 * colas, exchanges y bindings sin que los controladores toquen el broker.
 */
@Configuration
public class RabbitMQAdminConfig {

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
