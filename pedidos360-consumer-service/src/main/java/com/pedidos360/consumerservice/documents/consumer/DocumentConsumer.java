package com.pedidos360.consumerservice.documents.consumer;

import com.pedidos360.consumerservice.messaging.NonRetryableException;
import com.pedidos360.consumerservice.messaging.RechazoMensaje;
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
 * Consumidor del dominio de generación de documentos. Confirma manualmente (ACK) y
 * decide la ruta de error de forma explícita:
 * <ul>
 *   <li>Error irrecuperable ({@link NonRetryableException}) -> NACK requeue=false -> DLQ.</li>
 *   <li>Error transitorio -> un reintento (requeue); si vuelve a fallar -> DLQ ({@link RechazoMensaje}).</li>
 * </ul>
 */
@Slf4j
@Component
public class DocumentConsumer {

    @RabbitListener(queues = "${pedidos360.rabbitmq.queue-documentos}")
    public void onMessage(
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("[documentos] RECIBIDO deliveryTag={} payload={}", deliveryTag, payload);

        try {
            process(payload);
            channel.basicAck(deliveryTag, false);
            log.info("[documentos] PROCESADO_OK deliveryTag={}", deliveryTag);
        } catch (Exception ex) {
            RechazoMensaje.rechazar(channel, message, deliveryTag, ex, "documentos");
        }
    }

    private void process(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new NonRetryableException("payload vacío");
        }
        log.debug("[documentos] generando documento: {}", payload);
    }
}
