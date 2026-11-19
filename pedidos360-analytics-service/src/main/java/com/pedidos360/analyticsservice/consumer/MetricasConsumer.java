package com.pedidos360.analyticsservice.consumer;

import com.pedidos360.analyticsservice.dto.MetricasPedidoDto;
import com.pedidos360.analyticsservice.service.ReportingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumer group {@code pedidos360-reporting-group} sobre el tópico de métricas
 * agregadas. Representa una responsabilidad distinta (reportes/BI) que escala de
 * forma independiente al grupo de analítica. La lógica vive en
 * {@link ReportingService}; este componente solo adapta la mensajería.
 */
@Slf4j
@Component
public class MetricasConsumer {

    private final ReportingService reportingService;

    public MetricasConsumer(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @KafkaListener(
            topics = "${pedidos360.kafka.topic-analitica-metricas}",
            groupId = "${pedidos360.kafka.consumer-group-reporting}",
            containerFactory = "metricasKafkaListenerContainerFactory")
    public void onMetrica(
            @Payload MetricasPedidoDto metrica,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        boolean actualizado = reportingService.aplicar(metrica);
        log.info("[reporting] metrica key={} particion={} offset={} estado={} totalPedidos={} totalVendido={} actualizado={}",
                key, partition, offset, metrica.estado(), metrica.totalPedidos(), metrica.totalVendido(), actualizado);
    }
}
