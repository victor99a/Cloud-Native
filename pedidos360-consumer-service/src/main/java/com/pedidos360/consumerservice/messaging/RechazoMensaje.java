package com.pedidos360.consumerservice.messaging;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;

/**
 * Política común de rechazo (NACK) para los consumidores RabbitMQ con ACK manual.
 *
 * <ul>
 *   <li>Error irrecuperable ({@link NonRetryableException}) → NACK requeue=false → DLQ.</li>
 *   <li>Error transitorio en la primera entrega → NACK requeue=true → un reintento.</li>
 *   <li>Error transitorio en un mensaje ya reentregado ({@code redelivered=true}) →
 *       NACK requeue=false → DLQ. Así un fallo persistente no queda reencolándose
 *       para siempre (bucle de reintentos que bloquea la cola).</li>
 * </ul>
 */
@Slf4j
public final class RechazoMensaje {

    private RechazoMensaje() {
    }

    /**
     * Rechaza el mensaje según el tipo de error y si ya fue reentregado.
     *
     * @return {@code true} si el mensaje se reencoló para reintentar; {@code false} si fue a la DLQ.
     */
    public static boolean rechazar(Channel channel, Message message, long deliveryTag,
                                   Exception error, String dominio) throws IOException {
        if (error instanceof NonRetryableException) {
            log.error("[{}] IRRECUPERABLE -> DLQ deliveryTag={} error={}",
                    dominio, deliveryTag, error.getMessage(), error);
            channel.basicNack(deliveryTag, false, false);
            return false;
        }
        if (Boolean.TRUE.equals(message.getMessageProperties().getRedelivered())) {
            log.error("[{}] Reintento agotado -> DLQ deliveryTag={} error={}",
                    dominio, deliveryTag, error.getMessage(), error);
            channel.basicNack(deliveryTag, false, false);
            return false;
        }
        log.warn("[{}] Transitorio -> reintento deliveryTag={} error={}",
                dominio, deliveryTag, error.getMessage());
        channel.basicNack(deliveryTag, false, true);
        return true;
    }
}
