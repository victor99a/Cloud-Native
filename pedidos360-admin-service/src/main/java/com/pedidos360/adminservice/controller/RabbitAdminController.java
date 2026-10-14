package com.pedidos360.adminservice.controller;

import com.pedidos360.adminservice.dto.BindingRequest;
import com.pedidos360.adminservice.dto.ExchangeRequest;
import com.pedidos360.adminservice.dto.QueueRequest;
import com.pedidos360.adminservice.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST del microservicio administrador de RabbitMQ.
 * Rutas y verbos HTTP claros: POST/DELETE de queues, exchanges y bindings.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Validated
public class RabbitAdminController {

    private final RabbitAdminService adminService;

    public RabbitAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    // ===== Colas =====

    @PostMapping("/queues")
    public ResponseEntity<Map<String, String>> createQueue(@Valid @RequestBody QueueRequest request) {
        String name = adminService.declareQueue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("queue", name));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> deleteQueue(@PathVariable @NotBlank String name) {
        adminService.deleteQueue(name);
        return ResponseEntity.noContent().build();
    }

    // ===== Exchanges =====

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> createExchange(@Valid @RequestBody ExchangeRequest request) {
        String name = adminService.declareExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("exchange", name));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> deleteExchange(@PathVariable @NotBlank String name) {
        adminService.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }

    // ===== Bindings =====

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, String>> createBinding(@Valid @RequestBody BindingRequest request) {
        String binding = adminService.declareBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("binding", binding));
    }
}
