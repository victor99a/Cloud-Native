package com.pedidos360.kafkaadminservice.service;

import com.pedidos360.kafkaadminservice.dto.ConsumerGroupInfo;
import com.pedidos360.kafkaadminservice.dto.TopicConfigRequest;
import com.pedidos360.kafkaadminservice.dto.TopicInfo;
import com.pedidos360.kafkaadminservice.dto.TopicRequest;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;
import org.springframework.stereotype.Service;

/**
 * Encapsula la gestión programática de Kafka (tópicos y consumer groups) a
 * través de {@link AdminClient}. Los controladores solo invocan estos métodos de
 * alto nivel y no manipulan el clúster directamente.
 */
@Slf4j
@Service
public class KafkaAdminService {

    private final AdminClient adminClient;

    public KafkaAdminService(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    // ===== Tópicos =====

    public String createTopic(TopicRequest request) {
        // Por defecto 3 particiones / 3 réplicas: tolerancia a fallos + paralelismo,
        // alineado con el clúster de 3 brokers y la rúbrica EP5.
        int partitions = request.partitions() != null ? request.partitions() : 3;
        short replicas = (short) (request.replicas() != null ? request.replicas() : 3);

        NewTopic topic = new NewTopic(request.name(), partitions, replicas);
        if (request.configs() != null) {
            topic.configs(request.configs());
        }

        await(adminClient.createTopics(List.of(topic)).all());
        log.info("[kafka-admin] tópico creado: {} (particiones={}, réplicas={})",
                request.name(), partitions, replicas);
        return request.name();
    }

    public List<String> listTopics() {
        return await(adminClient.listTopics().listings()).stream()
                .map(TopicListing::name)
                .sorted()
                .toList();
    }

    public TopicInfo describeTopic(String name) {
        TopicDescription description = await(adminClient.describeTopics(List.of(name)).allTopicNames())
                .get(name);
        if (description == null) {
            throw new IllegalArgumentException("Tópico no encontrado: " + name);
        }

        Map<String, String> configs = describeConfigs(name);

        int partitions = description.partitions().size();
        int replicas = partitions > 0 ? description.partitions().get(0).replicas().size() : 0;

        List<TopicInfo.PartitionInfo> partitionInfos = description.partitions().stream()
                .map(p -> new TopicInfo.PartitionInfo(
                        p.partition(),
                        p.leader() != null ? p.leader().id() : null,
                        p.replicas().stream().map(n -> n.id()).toList(),
                        p.isr().stream().map(n -> n.id()).toList()))
                .toList();

        return new TopicInfo(name, partitions, replicas, configs, partitionInfos);
    }

    public void alterTopicConfigs(String name, TopicConfigRequest request) {
        ConfigResource resource = new ConfigResource(Type.TOPIC, name);
        Collection<AlterConfigOp> ops = request.configs().entrySet().stream()
                .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()),
                        AlterConfigOp.OpType.SET))
                .toList();
        await(adminClient.incrementalAlterConfigs(Map.of(resource, ops)).all());
        log.info("[kafka-admin] configuración alterada del tópico {}: {}", name, request.configs());
    }

    public void deleteTopic(String name) {
        await(adminClient.deleteTopics(List.of(name)).all());
        log.info("[kafka-admin] tópico eliminado: {}", name);
    }

    // ===== Consumer groups =====

    public List<String> listConsumerGroups() {
        return await(adminClient.listConsumerGroups().all()).stream()
                .map(group -> group.groupId())
                .sorted()
                .toList();
    }

    public ConsumerGroupInfo describeConsumerGroup(String groupId) {
        ConsumerGroupDescription description =
                await(adminClient.describeConsumerGroups(List.of(groupId)).all()).get(groupId);
        if (description == null) {
            throw new IllegalArgumentException("Consumer group no encontrado: " + groupId);
        }

        List<ConsumerGroupInfo.MemberInfo> members = description.members().stream()
                .map(this::toMemberInfo)
                .toList();

        return new ConsumerGroupInfo(
                description.groupId(),
                description.isSimpleConsumerGroup(),
                description.state() != null ? description.state().name() : null,
                members);
    }

    public void deleteConsumerGroup(String groupId) {
        await(adminClient.deleteConsumerGroups(List.of(groupId)).all());
        log.info("[kafka-admin] consumer group eliminado: {}", groupId);
    }

    // ===== Helpers =====

    private ConsumerGroupInfo.MemberInfo toMemberInfo(MemberDescription member) {
        List<String> partitions = member.assignment().topicPartitions().stream()
                .map(tp -> tp.topic() + "-" + tp.partition())
                .sorted()
                .toList();
        return new ConsumerGroupInfo.MemberInfo(
                member.consumerId(),
                member.clientId(),
                member.host(),
                partitions);
    }

    private Map<String, String> describeConfigs(String topic) {
        ConfigResource resource = new ConfigResource(Type.TOPIC, topic);
        Config config = await(adminClient.describeConfigs(List.of(resource)).all()).get(resource);
        if (config == null) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        for (ConfigEntry entry : config.entries()) {
            result.put(entry.name(), entry.value());
        }
        return result;
    }

    private static <T> T await(KafkaFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación interrumpida", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Error al operar sobre Kafka: " + e.getCause().getMessage(), e.getCause());
        }
    }
}
