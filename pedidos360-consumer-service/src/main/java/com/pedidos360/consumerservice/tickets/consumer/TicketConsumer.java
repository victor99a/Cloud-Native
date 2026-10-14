package com.pedidos360.consumerservice.tickets.consumer;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumidor del dominio de emisión de tickets. Confirma manualmente (ACK) y
 * desvía los fallos irrecuperables a la DLQ (NACK, requeue=false).
 */
@Slf4j
@Component
public class TicketConsumer {

    @RabbitListener(queues = "${pedidos360.rabbitmq.queue-tickets}")
    public void onMessage(
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("[tickets] RECIBIDO deliveryTag={} payload={}", deliveryTag, payload);

        try {
            process(payload);
            channel.basicAck(deliveryTag, false);
            log.info("[tickets] PROCESADO_OK deliveryTag={}", deliveryTag);
        } catch (Exception ex) {
            log.error("[tickets] FALLO irrecuperable -> DLQ deliveryTag={} error={}",
                    deliveryTag, ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void process(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload vacío");
        }
        log.debug("[tickets] emitiendo ticket: {}", payload);
    }
}
