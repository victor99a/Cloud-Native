package com.pedidos360.analyticsservice;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pedidos360.analyticsservice.consumer.MetricasConsumer;
import com.pedidos360.analyticsservice.service.ReportingService;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback;

/** La vista de reporting se reconstruye leyendo el tópico compactado desde el inicio. */
class MetricasConsumerTest {

    @Test
    void alAsignarseParticionesReleeElTopicoDesdeElInicio() {
        MetricasConsumer consumer = new MetricasConsumer(new ReportingService());
        ConsumerSeekCallback callback = mock(ConsumerSeekCallback.class);
        Map<TopicPartition, Long> asignadas = Map.of(
                new TopicPartition("pedidos360.analitica.metricas", 0), 27L,
                new TopicPartition("pedidos360.analitica.metricas", 2), 5L);

        consumer.onPartitionsAssigned(asignadas, callback);

        verify(callback).seekToBeginning(asignadas.keySet());
    }
}
