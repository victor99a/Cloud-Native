package com.pedidos360.consumerservice.streaming.consumer;

import com.pedidos360.consumerservice.streaming.dto.PedidoEventDto;
import com.pedidos360.consumerservice.streaming.service.AlertaPedidoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumer group {@code pedidos360-alertas-group} sobre el tópico de eventos de
 * pedidos. Comparte el tópico con {@code pedidos360-analytics-group} (otro
 * microservicio): cada grupo recibe todos los eventos y cumple una
 * responsabilidad distinta (alertas vs. métricas).
 */
@Slf4j
@Component
public class PedidoAlertasConsumer {

    private final AlertaPedidoService alertaService;

    public PedidoAlertasConsumer(AlertaPedidoService alertaService) {
        this.alertaService = alertaService;
    }

    @KafkaListener(
            topics = "${pedidos360.kafka.topic-pedidos-eventos}",
            groupId = "${pedidos360.kafka.consumer-group-alertas}")
    public void onPedidoEvent(
            @Payload PedidoEventDto evento,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("[alertas] recibido key={} particion={} offset={} estado={} total={}",
                key, partition, offset, evento.estado(), evento.total());
        alertaService.evaluar(evento);
    }
}
