package com.pedidos360.pedidosservice.messaging;

import com.pedidos360.pedidosservice.dto.response.PedidoResponse;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor de eventos de dominio. Publica a través de {@link RabbitTemplate}
 * (abstracción inyectada) sin conocer detalles de bajo nivel del broker.
 *
 * <p>Publica objetos {@link PedidoEvent} tipados (serializados a JSON por el
 * conversor configurado) y asocia un {@code correlationId} para trazabilidad.
 */
@Slf4j
@Component
public class PedidoEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProducerProperties props;

    public PedidoEventPublisher(
            RabbitTemplate rabbitTemplate,
            RabbitMQProducerProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    /** Publica el evento de pedido creado en el exchange topic y en el direct (ticket). */
    public void publishPedidoCreado(PedidoResponse pedido) {
        PedidoEvent event = toEvent(pedido);
        publish(props.exchangeTopic(), props.routingPedidoCreado(), event);
        publish(props.exchangeDirect(), props.routingPedidoCreado(), event);
        log.info("[productor] pedido.creado publicado para pedido {}", pedido.numero());
    }

    /** Publica el evento de cambio de estado en el exchange topic con routing key por estado. */
    public void publishEstadoCambiado(PedidoResponse pedido) {
        String routingKey = props.routingPedidoEstadoPrefix() + pedido.estado().name().toLowerCase();
        publish(props.exchangeTopic(), routingKey, toEvent(pedido));
        log.info("[productor] {} publicado para pedido {}", routingKey, pedido.numero());
    }

    private void publish(String exchange, String routingKey, PedidoEvent event) {
        CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(exchange, routingKey, event, msg -> {
            msg.getMessageProperties().setCorrelationId(correlation.getId());
            msg.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
            return msg;
        }, correlation);
        log.info("[productor] {} -> {} correlationId={}", routingKey, exchange, correlation.getId());
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
}
