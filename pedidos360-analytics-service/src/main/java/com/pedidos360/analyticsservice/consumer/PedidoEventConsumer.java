package com.pedidos360.analyticsservice.consumer;

import com.pedidos360.analyticsservice.dto.PedidoEventDto;
import com.pedidos360.analyticsservice.service.AnalyticsService;
import com.pedidos360.analyticsservice.service.MetricasProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor principal: lee el tópico de eventos de pedidos dentro del consumer
 * group {@code pedidos360-analytics-group}. Agrega la métrica y publica el
 * resultado en el tópico de métricas.
 *
 * <p>El manejo de errores (validación, reintentos, DLT) lo gestiona el
 * {@code DefaultErrorHandler} configurado en {@code KafkaConsumerConfig}.
 */
@Slf4j
@Component
public class PedidoEventConsumer {

    private final AnalyticsService analyticsService;
    private final MetricasProducer metricasProducer;

    public PedidoEventConsumer(AnalyticsService analyticsService, MetricasProducer metricasProducer) {
        this.analyticsService = analyticsService;
        this.metricasProducer = metricasProducer;
    }

    @KafkaListener(
            topics = "${pedidos360.kafka.topic-pedidos-eventos}",
            groupId = "${pedidos360.kafka.consumer-group-analytics}")
    public void onPedidoEvent(
            @Payload PedidoEventDto evento,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("[analitica] recibido key={} particion={} offset={} pedidoId={} tipo={} estado={} eventId={}",
                key, partition, offset, evento.pedidoId(), evento.tipoEvento(), evento.estado(), evento.eventId());

        // Se publica la métrica de CADA estado afectado (anterior y nuevo).
        analyticsService.registrarEvento(evento).forEach(metricasProducer::publicarMetrica);
    }
}
