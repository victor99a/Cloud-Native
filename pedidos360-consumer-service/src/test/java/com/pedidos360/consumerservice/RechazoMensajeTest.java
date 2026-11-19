package com.pedidos360.consumerservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pedidos360.consumerservice.messaging.NonRetryableException;
import com.pedidos360.consumerservice.messaging.RechazoMensaje;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * Política de rechazo: irrecuperable → DLQ; transitorio → un solo reintento;
 * transitorio en un mensaje ya reentregado → DLQ (sin bucle infinito).
 */
class RechazoMensajeTest {

    private final Channel channel = mock(Channel.class);

    @Test
    void irrecuperableVaDirectoALaDlq() throws Exception {
        boolean reencolado = RechazoMensaje.rechazar(channel, mensaje(false), 1L,
                new NonRetryableException("payload vacío"), "test");

        assertThat(reencolado).isFalse();
        verify(channel).basicNack(1L, false, false);
    }

    @Test
    void transitorioEnPrimeraEntregaSeReintenta() throws Exception {
        boolean reencolado = RechazoMensaje.rechazar(channel, mensaje(false), 2L,
                new IllegalStateException("servicio caído"), "test");

        assertThat(reencolado).isTrue();
        verify(channel).basicNack(2L, false, true);
    }

    @Test
    void transitorioYaReentregadoVaALaDlq() throws Exception {
        boolean reencolado = RechazoMensaje.rechazar(channel, mensaje(true), 3L,
                new IllegalStateException("servicio caído"), "test");

        assertThat(reencolado).isFalse();
        verify(channel).basicNack(3L, false, false);
    }

    private static Message mensaje(boolean reentregado) {
        MessageProperties props = new MessageProperties();
        props.setRedelivered(reentregado);
        return new Message("{}".getBytes(), props);
    }
}
