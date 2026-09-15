package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.PedidoServiceClient;
import com.pedidos360.bff.dto.request.EstadoPedidoRequest;
import com.pedidos360.bff.dto.request.PedidoRequest;
import com.pedidos360.bff.dto.response.PedidoResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoServiceClient pedidoServiceClient;

    public PedidoController(PedidoServiceClient pedidoServiceClient) {
        this.pedidoServiceClient = pedidoServiceClient;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<PedidoResponse> findAll() {
        return pedidoServiceClient.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PedidoResponse findById(@PathVariable Long id) {
        return pedidoServiceClient.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<PedidoResponse> create(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoServiceClient.create(request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public PedidoResponse updateEstado(@PathVariable Long id, @Valid @RequestBody EstadoPedidoRequest request) {
        return pedidoServiceClient.updateEstado(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pedidoServiceClient.delete(id);
        return ResponseEntity.noContent().build();
    }
}
