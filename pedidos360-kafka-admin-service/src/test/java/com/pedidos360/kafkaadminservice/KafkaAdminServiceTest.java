package com.pedidos360.kafkaadminservice;

import com.pedidos360.kafkaadminservice.dto.ConsumerGroupOffsets;
import com.pedidos360.kafkaadminservice.dto.PartitionsRequest;
import com.pedidos360.kafkaadminservice.dto.TopicRequest;
import com.pedidos360.kafkaadminservice.service.KafkaAdminService;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas básicas del servicio administrador de Kafka con un AdminClient simulado
 * (sin clúster real): valores por defecto de tópicos, cálculo de lag y
 * validación del aumento de particiones.
 */
class KafkaAdminServiceTest {

    private static final String TOPIC = "pedidos360.pedidos.eventos";

    private final AdminClient adminClient = mock(AdminClient.class);
    private final KafkaAdminService service = new KafkaAdminService(adminClient);

    @Test
    @SuppressWarnings("unchecked")
    void creaTopicoConTresParticionesYTresReplicasPorDefecto() {
        CreateTopicsResult result = mock(CreateTopicsResult.class);
        when(result.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.createTopics(anyCollection())).thenReturn(result);

        service.createTopic(new TopicRequest("nuevo.topico", null, null, Map.of("retention.ms", "1000")));

        ArgumentCaptor<java.util.Collection<NewTopic>> captor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(adminClient).createTopics(captor.capture());
        NewTopic topic = captor.getValue().iterator().next();
        assertThat(topic.name()).isEqualTo("nuevo.topico");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 3);
        assertThat(topic.configs()).containsEntry("retention.ms", "1000");
    }

    @Test
    void calculaLagPorParticionYTotal() {
        TopicPartition p0 = new TopicPartition(TOPIC, 0);
        TopicPartition p1 = new TopicPartition(TOPIC, 1);

        ListConsumerGroupOffsetsResult committed = mock(ListConsumerGroupOffsetsResult.class);
        when(committed.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(Map.of(
                p0, new OffsetAndMetadata(10),
                p1, new OffsetAndMetadata(5))));
        when(adminClient.listConsumerGroupOffsets("grupo")).thenReturn(committed);

        ListOffsetsResult end = mock(ListOffsetsResult.class);
        when(end.all()).thenReturn(KafkaFuture.completedFuture(Map.of(
                p0, new ListOffsetsResult.ListOffsetsResultInfo(12, -1L, java.util.Optional.empty()),
                p1, new ListOffsetsResult.ListOffsetsResultInfo(5, -1L, java.util.Optional.empty()))));
        when(adminClient.listOffsets(anyMap())).thenReturn(end);

        ConsumerGroupOffsets offsets = service.consumerGroupOffsets("grupo");

        assertThat(offsets.totalLag()).isEqualTo(2L);
        assertThat(offsets.partitions()).hasSize(2);
        assertThat(offsets.partitions().get(0).partition()).isZero();
        assertThat(offsets.partitions().get(0).lag()).isEqualTo(2L);
        assertThat(offsets.partitions().get(1).lag()).isZero();
    }

    @Test
    void rechazaReducirParticiones() {
        stubTopicoConParticiones(3);

        assertThatThrownBy(() -> service.increasePartitions(TOPIC, new PartitionsRequest(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("solo se puede aumentar");
        verify(adminClient, never()).createPartitions(anyMap());
    }

    private void stubTopicoConParticiones(int particiones) {
        Node broker = new Node(1, "kafka1", 29092);
        List<TopicPartitionInfo> infos = java.util.stream.IntStream.range(0, particiones)
                .mapToObj(i -> new TopicPartitionInfo(i, broker, List.of(broker), List.of(broker)))
                .toList();

        DescribeTopicsResult topics = mock(DescribeTopicsResult.class);
        when(topics.allTopicNames()).thenReturn(KafkaFuture.completedFuture(
                Map.of(TOPIC, new TopicDescription(TOPIC, false, infos))));
        when(adminClient.describeTopics(anyCollection())).thenReturn(topics);

        DescribeConfigsResult configs = mock(DescribeConfigsResult.class);
        when(configs.all()).thenReturn(KafkaFuture.completedFuture(Map.of(
                new ConfigResource(ConfigResource.Type.TOPIC, TOPIC), new Config(List.of()))));
        when(adminClient.describeConfigs(anyCollection())).thenReturn(configs);
    }
}
