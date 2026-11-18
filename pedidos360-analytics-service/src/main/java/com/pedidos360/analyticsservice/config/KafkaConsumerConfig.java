package com.pedidos360.analyticsservice.config;

import com.pedidos360.analyticsservice.dto.MetricasPedidoDto;
import com.pedidos360.analyticsservice.dto.PedidoEventDto;
import com.pedidos360.analyticsservice.exception.NonRetryableEventException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Configuración de los consumidores Kafka (cliente).
 *
 * <p>Puntos clave:
 * <ul>
 *   <li><b>ErrorHandlingDeserializer</b> envuelve al {@link JsonDeserializer}: un
 *       payload que ni siquiera es JSON válido (poison pill) no bloquea la
 *       partición; se convierte en una {@code DeserializationException} que el
 *       error handler envía directo a la DLT sin reintentos.</li>
 *   <li>Commit de offsets controlado por el contenedor (auto-commit desactivado,
 *       {@code AckMode.RECORD}): el offset se confirma solo después de procesar
 *       cada registro con éxito (o de desviarlo a la DLT).</li>
 *   <li>Paralelismo: 3 hilos consumidores = 3 particiones por tópico.</li>
 *   <li>{@link DefaultErrorHandler} con 3 reintentos (backoff 1 s) y
 *       {@link DeadLetterPublishingRecoverer} hacia la Dead Letter Topic.</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private final String bootstrapServers;
    private final KafkaProperties props;

    public KafkaConsumerConfig(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            KafkaProperties props) {
        this.bootstrapServers = bootstrapServers;
        this.props = props;
    }

    @Bean
    public ConsumerFactory<String, PedidoEventDto> pedidoEventConsumerFactory() {
        return jsonConsumerFactory(PedidoEventDto.class);
    }

    @Bean
    public ConsumerFactory<String, MetricasPedidoDto> metricasConsumerFactory() {
        return jsonConsumerFactory(MetricasPedidoDto.class);
    }

    /** Listener principal de eventos de pedidos (consumer group de analítica). */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PedidoEventDto> kafkaListenerContainerFactory(
            ConsumerFactory<String, PedidoEventDto> pedidoEventConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, PedidoEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(pedidoEventConsumerFactory);
        factory.setConcurrency(3); // balanceo de carga: una partición por hilo consumidor
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    /**
     * Listener del tópico de métricas (consumer group "reporting"),
     * responsabilidad distinta que escala de forma independiente.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MetricasPedidoDto>
            metricasKafkaListenerContainerFactory(
            ConsumerFactory<String, MetricasPedidoDto> metricasConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, MetricasPedidoDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(metricasConsumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    /**
     * Listener de la Dead Letter Topic. Lee el valor como texto plano para poder
     * registrar cualquier payload (incluso uno que no es JSON válido). No usa el
     * error handler con DLT para evitar ciclos: si falla, solo se registra.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dltKafkaListenerContainerFactory() {
        Map<String, Object> config = baseConsumerConfig();
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config));
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                (record, ex) -> log.error("[dlt-monitor] no se pudo registrar mensaje de la DLT offset={} error={}",
                        record.offset(), ex.getMessage()),
                new FixedBackOff(0L, 0L)));
        return factory;
    }

    /**
     * Resiliencia: reintenta 3 veces con backoff fijo (1 s); si el mensaje sigue
     * fallando lo publica en la Dead Letter Topic preservando su partición
     * original. Los errores irrecuperables (payload inválido o no deserializable)
     * van directo a la DLT sin reintentos.
     *
     * <p>Se usan dos plantillas: una JSON para valores ya deserializados y otra
     * de bytes para los payloads crudos que fallaron al deserializar.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Qualifier("dltBytesKafkaTemplate") KafkaTemplate<String, byte[]> dltBytesKafkaTemplate) {
        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, dltBytesKafkaTemplate);
        templates.put(Object.class, kafkaTemplate);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                templates,
                (record, ex) -> {
                    log.error("[kafka-error] enviando a DLT topic={} particion={} offset={} key={} causa={}",
                            record.topic(), record.partition(), record.offset(), record.key(),
                            ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    return new TopicPartition(props.topicPedidosEventosDlt(), record.partition());
                });

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        // Un payload inválido nunca se recupera: va directo a la DLT sin reintentos.
        // (DeserializationException ya viene marcada como no reintentable por defecto.)
        handler.addNotRetryableExceptions(NonRetryableEventException.class);
        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("[kafka-error] reintento {} topic={} particion={} offset={} error={}",
                        deliveryAttempt, record.topic(), record.partition(), record.offset(),
                        ex.getMessage()));
        return handler;
    }

    private <T> ConsumerFactory<String, T> jsonConsumerFactory(Class<T> targetType) {
        Map<String, Object> config = baseConsumerConfig();
        // ErrorHandlingDeserializer -> delega en JsonDeserializer; si falla, no rompe el poll().
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        // JSON tipado sin encabezados de tipo: interoperable entre servicios.
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, targetType.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.pedidos360.*");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    private Map<String, Object> baseConsumerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        return config;
    }
}
