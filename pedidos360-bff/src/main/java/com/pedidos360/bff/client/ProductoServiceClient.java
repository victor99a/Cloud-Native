package com.pedidos360.bff.client;

import com.pedidos360.bff.dto.request.ProductoRequest;
import com.pedidos360.bff.dto.response.ProductoResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente tipado para el microservicio de productos (pedidos360-productos-service).
 */
@Component
public class ProductoServiceClient {

    private final RestClient restClient;

    public ProductoServiceClient(@Qualifier("productosRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ProductoResponse> findAll() {
        return restClient.get()
                .uri("/api/productos")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ProductoResponse findById(Long id) {
        return restClient.get()
                .uri("/api/productos/{id}", id)
                .retrieve()
                .body(ProductoResponse.class);
    }

    public ProductoResponse create(ProductoRequest req) {
        return restClient.post()
                .uri("/api/productos")
                .body(req)
                .retrieve()
                .body(ProductoResponse.class);
    }

    public ProductoResponse update(Long id, ProductoRequest req) {
        return restClient.put()
                .uri("/api/productos/{id}", id)
                .body(req)
                .retrieve()
                .body(ProductoResponse.class);
    }

    public void delete(Long id) {
        restClient.delete()
                .uri("/api/productos/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
