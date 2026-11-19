package com.pedidos360.consumerservice.notifications.consumer;

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
 * Consumidor del dominio de notificaciones. Confirma manualmente cada mensaje (ACK)
 * y decide de forma explícita la ruta de error:
 * <ul>
 *   <li>Error irrecuperable ({@link NonRetryableException}) -> NACK requeue=false -> DLQ.</li>
 *   <li>Error transitorio -> un reintento (requeue); si vuelve a fallar -> DLQ ({@link RechazoMensaje}).</li>
 * </ul>
 */
@Slf4j
@Component
public class NotificationConsumer {

    @RabbitListener(queues = "${pedidos360.rabbitmq.queue-notificaciones}")
    public void onMessage(
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("[notificaciones] RECIBIDO deliveryTag={} payload={}", deliveryTag, payload);

        try {
            process(payload);
            channel.basicAck(deliveryTag, false);
            log.info("[notificaciones] PROCESADO_OK deliveryTag={}", deliveryTag);
        } catch (Exception ex) {
            RechazoMensaje.rechazar(channel, message, deliveryTag, ex, "notificaciones");
        }
    }

    private void process(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new NonRetryableException("payload vacío");
        }
        // Aquí iría el envío real de la notificación (email/push).
        log.debug("[notificaciones] procesando notificación: {}", payload);
    }
}
