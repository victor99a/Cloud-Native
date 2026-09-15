package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.ProductoServiceClient;
import com.pedidos360.bff.dto.request.ProductoRequest;
import com.pedidos360.bff.dto.response.ProductoResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoServiceClient productoServiceClient;

    public ProductoController(ProductoServiceClient productoServiceClient) {
        this.productoServiceClient = productoServiceClient;
    }

    @GetMapping
    public List<ProductoResponse> findAll() {
        return productoServiceClient.findAll();
    }

    @GetMapping("/{id}")
    public ProductoResponse findById(@PathVariable Long id) {
        return productoServiceClient.findById(id);
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> create(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoServiceClient.create(request));
    }

    @PutMapping("/{id}")
    public ProductoResponse update(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return productoServiceClient.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productoServiceClient.delete(id);
        return ResponseEntity.noContent().build();
    }
}
