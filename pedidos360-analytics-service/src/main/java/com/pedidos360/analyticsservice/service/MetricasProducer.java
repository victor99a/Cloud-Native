package com.pedidos360.analyticsservice.service;

import com.pedidos360.analyticsservice.config.KafkaProperties;
import com.pedidos360.analyticsservice.dto.MetricasPedidoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Productor de métricas agregadas hacia el tópico {@code pedidos360.analitica.metricas}.
 * La clave del mensaje es el {@code estado}, garantizando que las métricas de un
 * mismo estado se ordenen y compacten en la misma partición.
 */
@Slf4j
@Component
public class MetricasProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProperties props;

    public MetricasProducer(KafkaTemplate<String, Object> kafkaTemplate, KafkaProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public void publicarMetrica(MetricasPedidoDto metrica) {
        kafkaTemplate.send(props.topicAnaliticaMetricas(), metrica.estado(), metrica)
                .whenComplete((SendResult<String, Object> result, Throwable ex) -> {
                    if (ex != null) {
                        log.error("[metrica] fallo al publicar estado={} error={}",
                                metrica.estado(), ex.getMessage(), ex);
                    } else {
                        log.info("[metrica] publicada estado={} particion={} offset={}",
                                metrica.estado(),
                                result.getRecordMetadata() != null
                                        ? result.getRecordMetadata().partition() : -1,
                                result.getRecordMetadata() != null
                                        ? result.getRecordMetadata().offset() : -1);
                    }
                });
    }
}
