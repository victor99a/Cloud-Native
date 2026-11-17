package com.pedidos360.pedidosservice.messaging;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Configuración del productor Kafka de eventos de pedidos.
 *
 * <p>Serializa {@link PedidoEvent} a JSON sin encabezados de tipo (interoperable
 * con el consumer del analytics-service) y publica con ACK de todas las réplicas
 * ISR + idempotencia para una entrega fiable.
 */
@Configuration
@EnableConfigurationProperties(KafkaProducerProperties.class)
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, PedidoEvent> pedidoEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, PedidoEvent> pedidoEventKafkaTemplate(
            ProducerFactory<String, PedidoEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
