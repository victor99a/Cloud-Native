package com.pedidos360.consumerservice.streaming.config;

import com.pedidos360.consumerservice.messaging.NonRetryableException;
import com.pedidos360.consumerservice.streaming.dto.PedidoEventDto;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Cliente Kafka del consumer-service (consumer group de alertas).
 *
 * <p>Este grupo lee el MISMO tópico que el grupo de analítica: Kafka entrega
 * cada evento a ambos grupos (fan-out), y dentro de cada grupo reparte las 3
 * particiones entre sus hilos (balanceo). Los mensajes defectuosos se registran
 * y se saltan sin bloquear la partición: la DLT del sistema la gestiona el
 * analytics-service.
 */
@Slf4j
@Configuration
@EnableKafka
@EnableConfigurationProperties(KafkaStreamingProperties.class)
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, PedidoEventDto> pedidoEventConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PedidoEventDto.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.pedidos360.*");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // "latest": las alertas solo tienen sentido para eventos nuevos, no históricos.
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PedidoEventDto> kafkaListenerContainerFactory(
            ConsumerFactory<String, PedidoEventDto> pedidoEventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, PedidoEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(pedidoEventConsumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // 2 reintentos (500 ms); luego se registra y se salta el mensaje.
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, ex) -> log.error("[alertas] DESCARTADO tras reintentos topic={} particion={} offset={} key={} causa={}",
                        record.topic(), record.partition(), record.offset(), record.key(),
                        ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()),
                new FixedBackOff(500L, 2L));
        handler.addNotRetryableExceptions(NonRetryableException.class);
        factory.setCommonErrorHandler(handler);
        return factory;
    }
}
