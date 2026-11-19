package com.pedidos360.adminservice;

import com.pedidos360.adminservice.dto.BindingRequest;
import com.pedidos360.adminservice.dto.ExchangeRequest;
import com.pedidos360.adminservice.dto.QueueRequest;
import com.pedidos360.adminservice.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pruebas básicas del administrador de RabbitMQ con un AmqpAdmin simulado.
 */
class RabbitAdminServiceTest {

    private final AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
    private final RabbitAdminService service = new RabbitAdminService(amqpAdmin);

    @Test
    void declaraColaDurablePorDefecto() {
        service.declareQueue(new QueueRequest("demo.queue", null, null, null, null));

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("demo.queue");
        assertThat(captor.getValue().isDurable()).isTrue();
    }

    @Test
    void declaraExchangeTopic() {
        service.declareExchange(new ExchangeRequest("demo.topic", "topic", null, null));

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(ExchangeTypes.TOPIC);
    }

    @Test
    void rechazaTipoDeExchangeInvalido() {
        assertThatThrownBy(() -> service.declareExchange(new ExchangeRequest("x", "raro", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(amqpAdmin);
    }

    @Test
    void eliminaCola() {
        service.deleteQueue("demo.queue");
        verify(amqpAdmin).deleteQueue("demo.queue");
    }

    @Test
    void eliminaBindingColaExchange() {
        service.removeBinding(new BindingRequest("demo.queue", "demo.exchange", "demo.key", null));

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).removeBinding(captor.capture());
        assertThat(captor.getValue().getDestination()).isEqualTo("demo.queue");
        assertThat(captor.getValue().getExchange()).isEqualTo("demo.exchange");
        assertThat(captor.getValue().getRoutingKey()).isEqualTo("demo.key");
        assertThat(captor.getValue().getDestinationType()).isEqualTo(Binding.DestinationType.QUEUE);
    }
}
