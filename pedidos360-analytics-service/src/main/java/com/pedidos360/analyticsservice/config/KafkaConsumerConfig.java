package com.pedidos360.analyticsservice.config;

import com.pedidos360.analyticsservice.dto.MetricasPedidoDto;
import com.pedidos360.analyticsservice.dto.PedidoEventDto;
import com.pedidos360.analyticsservice.exception.NonRetryableEventException;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Configuración de los consumidores Kafka (cliente).
 *
 * <p>Define factories tipadas para cada tópico, con deserializador JSON sin
 * encabezados de tipo (desacoplamiento entre productor y consumidor), ACK
 * manual, paralelismo (3 hilos = 3 particiones) y un {@link DefaultErrorHandler}
 * con reintentos + Dead Letter Topic para mensajes defectuosos.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private final String bootstrapServers;
    private final KafkaProperties props;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaConsumerConfig(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            KafkaProperties props,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.bootstrapServers = bootstrapServers;
        this.props = props;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Bean
    public ConsumerFactory<String, PedidoEventDto> pedidoEventConsumerFactory() {
        return consumerFactory(PedidoEventDto.class);
    }

    @Bean
    public ConsumerFactory<String, MetricasPedidoDto> metricasConsumerFactory() {
        return consumerFactory(MetricasPedidoDto.class);
    }

    /** Listener principal de eventos de pedidos (consumer group de analítica). */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PedidoEventDto> kafkaListenerContainerFactory(
            ConsumerFactory<String, PedidoEventDto> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, PedidoEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3); // balanceo de carga: una partición por hilo consumidor
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    /**
     * Listener del tópico de métricas (segundo consumer group, "reporting"),
     * demostrando la distribución de responsabilidades entre grupos.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MetricasPedidoDto>
            metricasKafkaListenerContainerFactory(
            ConsumerFactory<String, MetricasPedidoDto> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, MetricasPedidoDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    private <T> ConsumerFactory<String, T> consumerFactory(Class<T> targetType) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        // Deserializador JSON tipado sin encabezados de tipo: interoperable entre servicios.
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, targetType);
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(
                config, new StringDeserializer(), new JsonDeserializer<T>());
    }

    /**
     * Resiliencia: reintenta 3 veces con backoff fijo (1 s); si el mensaje sigue
     * fallando (o es irrecuperable por deserialización/validación) lo publica en
     * el Dead Letter Topic preservando su partición original.
     */
    private DefaultErrorHandler errorHandler() {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(props.topicPedidosEventosDlt(), record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        // Un payload inválido nunca se recupera: va directo a la DLT sin reintentos.
        handler.addNotRetryableExceptions(NonRetryableEventException.class);
        return handler;
    }
}
