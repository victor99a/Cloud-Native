package com.pedidos360.analyticsservice.consumer;

import com.pedidos360.analyticsservice.dto.MetricasPedidoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Segundo consumer group ({@code pedidos360-reporting-group}) que consume el
 * tópico de métricas agregadas. Representa una responsabilidad distinta
 * (reportes/BI) que puede escalar de forma independiente al grupo de analítica,
 * demostrando la distribución de responsabilidades entre grupos.
 */
@Slf4j
@Component
public class MetricasConsumer {

    @KafkaListener(
            topics = "${pedidos360.kafka.topic-analitica-metricas}",
            groupId = "${pedidos360.kafka.consumer-group-reporting}",
            containerFactory = "metricasKafkaListenerContainerFactory")
    public void onMetrica(
            @Payload MetricasPedidoDto metrica,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("[reporting] metrica recibida key={} particion={} offset={} estado={} totalPedidos={} totalVendido={}",
                key, partition, offset, metrica.estado(), metrica.totalPedidos(), metrica.totalVendido());
        // Aquí se alimentaría un almacén de reportes / BI (fuera del alcance del demo).
    }
}
