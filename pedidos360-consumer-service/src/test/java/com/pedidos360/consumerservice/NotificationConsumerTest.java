package com.pedidos360.consumerservice;

import com.pedidos360.consumerservice.notifications.consumer.NotificationConsumer;
import com.rabbitmq.client.Channel;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * Pruebas del consumidor RabbitMQ de notificaciones: ACK en éxito y NACK sin
 * reencolar (-> DLQ) ante un mensaje irrecuperable.
 */
class NotificationConsumerTest {

    private final NotificationConsumer consumer = new NotificationConsumer();
    private final Channel channel = mock(Channel.class);

    @Test
    void mensajeValidoSeConfirmaConAck() throws Exception {
        consumer.onMessage(mensaje("{\"pedidoId\":1}"), channel, 7L);

        verify(channel).basicAck(7L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void mensajeVacioVaALaDlq() throws Exception {
        consumer.onMessage(mensaje(""), channel, 8L);

        verify(channel).basicNack(8L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private static Message mensaje(String body) {
        return new Message(body.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
