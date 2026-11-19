package com.pedidos360.pedidosservice.messaging;

import com.pedidos360.pedidosservice.dto.response.PedidoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Productor de eventos de pedidos hacia Kafka (tópico {@code pedidos360.pedidos.eventos}).
 * La clave del mensaje es el número del pedido, garantizando que los eventos de un
 * mismo pedido mantengan orden dentro de su partición.
 */
@Slf4j
@Component
public class PedidoEventKafkaProducer {

    private final KafkaTemplate<String, PedidoEvent> kafkaTemplate;
    private final KafkaProducerProperties props;

    public PedidoEventKafkaProducer(
            KafkaTemplate<String, PedidoEvent> kafkaTemplate,
            KafkaProducerProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public void publishPedidoEvent(TipoEventoPedido tipo, PedidoResponse pedido) {
        PedidoEvent event = PedidoEvent.of(tipo, pedido);
        kafkaTemplate.send(props.topicPedidosEventos(), pedido.numero(), event)
                .whenComplete((SendResult<String, PedidoEvent> result, Throwable ex) -> {
                    if (ex != null) {
                        log.error("[kafka-producer] fallo al publicar pedido={} tipo={} eventId={} error={}",
                                pedido.numero(), tipo, event.eventId(), ex.getMessage(), ex);
                    } else {
                        log.info("[kafka-producer] publicado pedido={} tipo={} estado={} eventId={} particion={} offset={}",
                                pedido.numero(), tipo, pedido.estado(), event.eventId(),
                                result.getRecordMetadata() != null ? result.getRecordMetadata().partition() : -1,
                                result.getRecordMetadata() != null ? result.getRecordMetadata().offset() : -1);
                    }
                });
    }
}
