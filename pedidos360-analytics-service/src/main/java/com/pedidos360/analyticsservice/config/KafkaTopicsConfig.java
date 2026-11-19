package com.pedidos360.analyticsservice.config;

import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Inicialización programática de tópicos (vía {@code KafkaAdmin}).
 *
 * <p>Refuerza la creación idempotente de los 2 tópicos con 3 particiones y 3
 * réplicas y sus políticas de retención/limpieza. Complementa (no reemplaza) al
 * script {@code infra/apps/kafka/create-topics.sh} para que el arranque sea
 * resiliente aunque el inicializador one-shot no se ejecute.
 */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic pedidosEventosTopic(KafkaProperties props) {
        return TopicBuilder.name(props.topicPedidosEventos())
                .partitions(3)
                .replicas(3)
                .configs(Map.of(
                        "retention.ms", "604800000", // 7 días
                        "cleanup.policy", "delete"))
                .build();
    }

    @Bean
    public NewTopic analiticaMetricasTopic(KafkaProperties props) {
        return TopicBuilder.name(props.topicAnaliticaMetricas())
                .partitions(3)
                .replicas(3)
                .configs(Map.of(
                        "retention.ms", "2592000000", // 30 días
                        "cleanup.policy", "compact,delete")) // último valor por clave (estado) y borrado tras 30 días
                .build();
    }

    /**
     * Dead Letter Topic (auxiliar de resiliencia): aquí se desvían los mensajes
     * defectuosos tras agotar los reintentos. No es un tópico de "flujo de datos",
     * sino parte del manejo de errores del consumidor.
     */
    @Bean
    public NewTopic pedidosEventosDltTopic(KafkaProperties props) {
        return TopicBuilder.name(props.topicPedidosEventosDlt())
                .partitions(3)
                .replicas(3)
                .configs(Map.of(
                        "retention.ms", "604800000",
                        "cleanup.policy", "delete"))
                .build();
    }
}
