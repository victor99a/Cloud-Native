package com.pedidos360.adminservice.service;

import com.pedidos360.adminservice.dto.BindingRequest;
import com.pedidos360.adminservice.dto.ExchangeRequest;
import com.pedidos360.adminservice.dto.QueueRequest;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

/**
 * Encapsula toda la interacción con la infraestructura de RabbitMQ usando
 * {@link AmqpAdmin}. Los controladores solo invocan estos métodos de alto nivel
 * y no manipulan objetos del broker directamente.
 */
@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public String declareQueue(QueueRequest request) {
        Queue queue = new Queue(
                request.name(),
                booleanOf(request.durable(), true),
                booleanOf(request.exclusive(), false),
                booleanOf(request.autoDelete(), false),
                request.arguments()
        );
        amqpAdmin.declareQueue(queue);
        return queue.getName();
    }

    public void deleteQueue(String name) {
        amqpAdmin.deleteQueue(name);
    }

    public String declareExchange(ExchangeRequest request) {
        Exchange exchange = buildExchange(request);
        amqpAdmin.declareExchange(exchange);
        return exchange.getName();
    }

    public void deleteExchange(String name) {
        amqpAdmin.deleteExchange(name);
    }

    public String declareBinding(BindingRequest request) {
        Binding binding = new Binding(
                request.queue(),
                Binding.DestinationType.QUEUE,
                request.exchange(),
                request.routingKey(),
                request.arguments()
        );
        amqpAdmin.declareBinding(binding);
        return request.queue() + " -> " + request.exchange() + " (" + request.routingKey() + ")";
    }

    private Exchange buildExchange(ExchangeRequest request) {
        String type = request.type().trim().toUpperCase();
        boolean durable = booleanOf(request.durable(), true);
        boolean autoDelete = booleanOf(request.autoDelete(), false);
        return switch (type) {
            case "DIRECT" -> new DirectExchange(request.name(), durable, autoDelete);
            case "TOPIC" -> new TopicExchange(request.name(), durable, autoDelete);
            case "FANOUT" -> new FanoutExchange(request.name(), durable, autoDelete);
            case "HEADERS" -> new HeadersExchange(request.name(), durable, autoDelete);
            default -> throw new IllegalArgumentException(
                    "Tipo de exchange no válido: '" + request.type() + "'. Usar direct, topic, fanout o headers.");
        };
    }

    private boolean booleanOf(Boolean value, boolean defaultValue) {
        return value != null ? value : defaultValue;
    }
}
