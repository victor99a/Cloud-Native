package com.pedidos360.pedidosservice.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.pedidosservice.dto.response.PedidoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor de eventos de dominio. Publica a través de {@link RabbitTemplate}
 * (abstracción inyectada) sin conocer detalles de bajo nivel del broker.
 */
@Slf4j
@Component
public class PedidoEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitMQProducerProperties props;

    public PedidoEventPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            RabbitMQProducerProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    /** Publica el evento de pedido creado en el exchange topic y en el direct (ticket). */
    public void publishPedidoCreado(PedidoResponse pedido) {
        String json = toJson(toEvent(pedido));
        rabbitTemplate.convertAndSend(props.exchangeTopic(), props.routingPedidoCreado(), json);
        rabbitTemplate.convertAndSend(props.exchangeDirect(), props.routingPedidoCreado(), json);
        log.info("[productor] pedido.creado publicado para pedido {}", pedido.numero());
    }

    /** Publica el evento de cambio de estado en el exchange topic con routing key por estado. */
    public void publishEstadoCambiado(PedidoResponse pedido) {
        String routingKey = props.routingPedidoEstadoPrefix() + pedido.estado().name().toLowerCase();
        rabbitTemplate.convertAndSend(props.exchangeTopic(), routingKey, toJson(toEvent(pedido)));
        log.info("[productor] {} publicado para pedido {}", routingKey, pedido.numero());
    }

    private PedidoEvent toEvent(PedidoResponse pedido) {
        return new PedidoEvent(
                pedido.id(),
                pedido.numero(),
                pedido.cliente(),
                pedido.estado().name(),
                pedido.total(),
                pedido.fecha()
        );
    }

    private String toJson(PedidoEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento del pedido", e);
        }
    }
}
