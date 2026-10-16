package com.pedidos360.pedidosservice.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la mensajería del productor.
 *
 * <p>Declara explícitamente los exchanges a los que publica (topología garantizada
 * aunque el consumer-service no esté levantado), un conversor JSON tipado y un
 * {@link RabbitTemplate} con publisher confirms/returns y correlación de mensajes.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RabbitMQProducerProperties.class)
public class RabbitMQProducerConfig {

    private final RabbitMQProducerProperties props;

    public RabbitMQProducerConfig(RabbitMQProducerProperties props) {
        this.props = props;
    }

    @Bean
    public TopicExchange pedidosTopicExchange() {
        return new TopicExchange(props.exchangeTopic(), true, false);
    }

    @Bean
    public DirectExchange pedidosDirectExchange() {
        return new DirectExchange(props.exchangeDirect(), true, false);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);

        // Si un mensaje no tiene ruta hacia ninguna cola, no se pierde en silencio.
        template.setMandatory(true);
        template.setReturnsCallback(returned -> log.error(
                "[productor] Mensaje sin ruta exchange={} routingKey={} reply={} {}",
                returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyCode(), returned.getReplyText()));

        // Publisher confirms: detectar publicaciones no confirmadas por el broker.
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("[productor] Publicación NO confirmada id={} cause={}",
                        correlationData != null ? correlationData.getId() : null, cause);
            }
        });
        return template;
    }
}
