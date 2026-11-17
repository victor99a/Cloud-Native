package com.pedidos360.kafkaadminservice.controller;

import com.pedidos360.kafkaadminservice.dto.ConsumerGroupInfo;
import com.pedidos360.kafkaadminservice.dto.TopicConfigRequest;
import com.pedidos360.kafkaadminservice.dto.TopicInfo;
import com.pedidos360.kafkaadminservice.dto.TopicRequest;
import com.pedidos360.kafkaadminservice.service.KafkaAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del microservicio administrador de Kafka. Permite crear, listar,
 * describir, modificar y eliminar tópicos, y administrar consumer groups.
 */
@RestController
@RequestMapping("/api/v1/kafka")
@Validated
public class KafkaAdminController {

    private final KafkaAdminService adminService;

    public KafkaAdminController(KafkaAdminService adminService) {
        this.adminService = adminService;
    }

    // ===== Tópicos =====

    @PostMapping("/topics")
    public ResponseEntity<Map<String, String>> createTopic(@Valid @RequestBody TopicRequest request) {
        String name = adminService.createTopic(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("topic", name));
    }

    @GetMapping("/topics")
    public List<String> listTopics() {
        return adminService.listTopics();
    }

    @GetMapping("/topics/{name}")
    public TopicInfo describeTopic(@PathVariable @NotBlank String name) {
        return adminService.describeTopic(name);
    }

    @PutMapping("/topics/{name}/configs")
    public ResponseEntity<Map<String, String>> alterTopicConfigs(
            @PathVariable @NotBlank String name,
            @Valid @RequestBody TopicConfigRequest request) {
        adminService.alterTopicConfigs(name, request);
        return ResponseEntity.ok(Map.of("topic", name, "status", "updated"));
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Void> deleteTopic(@PathVariable @NotBlank String name) {
        adminService.deleteTopic(name);
        return ResponseEntity.noContent().build();
    }

    // ===== Consumer groups =====

    @GetMapping("/consumer-groups")
    public List<String> listConsumerGroups() {
        return adminService.listConsumerGroups();
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ConsumerGroupInfo describeConsumerGroup(@PathVariable @NotBlank String groupId) {
        return adminService.describeConsumerGroup(groupId);
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    public ResponseEntity<Void> deleteConsumerGroup(@PathVariable @NotBlank String groupId) {
        adminService.deleteConsumerGroup(groupId);
        return ResponseEntity.noContent().build();
    }
}
