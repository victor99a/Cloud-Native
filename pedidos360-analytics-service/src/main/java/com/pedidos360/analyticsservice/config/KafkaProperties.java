package com.pedidos360.analyticsservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de tópicos y consumer groups centralizados en {@code application.yml}
 * (prefijo {@code pedidos360.kafka}). Cero hardcoding: ningún consumidor,
 * productor o servicio escribe literales de nombres de tópico/grupo.
 */
@ConfigurationProperties(prefix = "pedidos360.kafka")
public record KafkaProperties(
        String topicPedidosEventos,
        String topicAnaliticaMetricas,
        String topicPedidosEventosDlt,
        String consumerGroupAnalytics,
        String consumerGroupReporting,
        String consumerGroupDltMonitor
) {
}
