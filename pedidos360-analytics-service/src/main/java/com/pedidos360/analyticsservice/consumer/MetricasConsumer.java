package com.pedidos360.analyticsservice.consumer;

import com.pedidos360.analyticsservice.dto.MetricasPedidoDto;
import com.pedidos360.analyticsservice.service.ReportingService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumer group {@code pedidos360-reporting-group} sobre el tópico de métricas
 * agregadas. Representa una responsabilidad distinta (reportes/BI) que escala de
 * forma independiente al grupo de analítica. La lógica vive en
 * {@link ReportingService}; este componente solo adapta la mensajería.
 *
 * <p>La vista vive en memoria, así que al asignarse las particiones (arranque o
 * rebalanceo) se relee el tópico compactado desde el inicio: la compactación
 * conserva el último valor por estado y la vista se reconstruye completa. Sin
 * esto, tras un reinicio el grupo retomaría desde su último offset y la vista
 * quedaría vacía hasta la próxima métrica.
 */
@Slf4j
@Component
public class MetricasConsumer implements ConsumerSeekAware {

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

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        callback.seekToBeginning(assignments.keySet());
        log.info("[reporting] reconstruyendo la vista desde el inicio del tópico compactado particiones={}",
                assignments.keySet());
    }
}
