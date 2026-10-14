package com.pedidos360.consumerservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la infraestructura de mensajería (topología RabbitMQ).
 *
 * <p>Declara beans explícitos de {@link Queue}, {@link org.springframework.amqp.core.Exchange}
 * y {@link Binding} para cada caso de uso (notificaciones, tickets, documentos) y sus
 * respectivas Dead Letter Queues. La lógica de negocio NO se mezcla con esta configuración:
 * los servicios solo consumen interfaces/componentes ya configurados.
 */
@Configuration
@EnableConfigurationProperties(RabbitMQProperties.class)
public class RabbitMQConfig {

    private final RabbitMQProperties props;

    public RabbitMQConfig(RabbitMQProperties props) {
        this.props = props;
    }

    // ===== Exchanges =====

    @Bean
    public TopicExchange pedidosTopicExchange() {
        return new TopicExchange(props.exchangeTopic(), true, false);
    }

    @Bean
    public DirectExchange pedidosDirectExchange() {
        return new DirectExchange(props.exchangeDirect(), true, false);
    }

    @Bean
    public DirectExchange pedidosDlxExchange() {
        return new DirectExchange(props.exchangeDlx(), true, false);
    }

    // ===== Colas de negocio (con Dead Letter configurado) =====

    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(props.queueNotificaciones())
                .deadLetterExchange(props.exchangeDlx())
                .deadLetterRoutingKey(props.queueNotificacionesDlq())
                .build();
    }

    @Bean
    public Queue ticketsQueue() {
        return QueueBuilder.durable(props.queueTickets())
                .deadLetterExchange(props.exchangeDlx())
                .deadLetterRoutingKey(props.queueTicketsDlq())
                .build();
    }

    @Bean
    public Queue documentosQueue() {
        return QueueBuilder.durable(props.queueDocumentos())
                .deadLetterExchange(props.exchangeDlx())
                .deadLetterRoutingKey(props.queueDocumentosDlq())
                .build();
    }

    // ===== Dead Letter Queues =====

    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(props.queueNotificacionesDlq()).build();
    }

    @Bean
    public Queue ticketsDlq() {
        return QueueBuilder.durable(props.queueTicketsDlq()).build();
    }

    @Bean
    public Queue documentosDlq() {
        return QueueBuilder.durable(props.queueDocumentosDlq()).build();
    }

    // ===== Bindings: exchange topic =====

    @Bean
    public Binding notificacionesBinding() {
        return BindingBuilder.bind(notificacionesQueue())
                .to(pedidosTopicExchange())
                .with(props.routingPedidoTodos());
    }

    @Bean
    public Binding documentosBinding() {
        return BindingBuilder.bind(documentosQueue())
                .to(pedidosTopicExchange())
                .with(props.routingPedidoEntregado());
    }

    // ===== Bindings: exchange direct =====

    @Bean
    public Binding ticketsBinding() {
        return BindingBuilder.bind(ticketsQueue())
                .to(pedidosDirectExchange())
                .with(props.routingPedidoCreado());
    }

    // ===== Bindings: DLQ -> DLX =====

    @Bean
    public Binding notificacionesDlqBinding() {
        return BindingBuilder.bind(notificacionesDlq())
                .to(pedidosDlxExchange())
                .with(props.queueNotificacionesDlq());
    }

    @Bean
    public Binding ticketsDlqBinding() {
        return BindingBuilder.bind(ticketsDlq())
                .to(pedidosDlxExchange())
                .with(props.queueTicketsDlq());
    }

    @Bean
    public Binding documentosDlqBinding() {
        return BindingBuilder.bind(documentosDlq())
                .to(pedidosDlxExchange())
                .with(props.queueDocumentosDlq());
    }
}
