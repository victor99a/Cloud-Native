package com.pedidos360.analyticsservice.consumer;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

/**
 * Monitor de la Dead Letter Topic ({@code pedidos360.pedidos.eventos.dlt}).
 *
 * <p>Registra en logs, con contexto completo, cada mensaje que no pudo
 * procesarse: tópico/partición/offset de origen, clase y mensaje de la excepción
 * (encabezados que agrega el {@code DeadLetterPublishingRecoverer}) y el payload
 * crudo. Así los mensajes defectuosos quedan aislados y trazables sin
 * interrumpir el flujo principal.
 */
@Slf4j
@Component
public class DeadLetterConsumer {

    @KafkaListener(
            topics = "${pedidos360.kafka.topic-pedidos-eventos-dlt}",
            groupId = "${pedidos360.kafka.consumer-group-dlt-monitor}",
            containerFactory = "dltKafkaListenerContainerFactory")
    public void onDeadLetter(ConsumerRecord<String, String> record) {
        log.error("[dlt-monitor] MENSAJE EN DLT key={} dltParticion={} dltOffset={} "
                        + "origen={}-{}@{} excepcion={} causa={} payload={}",
                record.key(), record.partition(), record.offset(),
                header(record, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                headerInt(record, KafkaHeaders.DLT_ORIGINAL_PARTITION),
                headerLong(record, KafkaHeaders.DLT_ORIGINAL_OFFSET),
                header(record, KafkaHeaders.DLT_EXCEPTION_FQCN),
                header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE),
                record.value());
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header != null ? new String(header.value(), StandardCharsets.UTF_8) : "-";
    }

    private static String headerInt(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null || header.value().length != Integer.BYTES) {
            return "-";
        }
        return String.valueOf(java.nio.ByteBuffer.wrap(header.value()).getInt());
    }

    private static String headerLong(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null || header.value().length != Long.BYTES) {
            return "-";
        }
        return String.valueOf(java.nio.ByteBuffer.wrap(header.value()).getLong());
    }
}
