package com.pedidos360.bff.client;

import com.pedidos360.bff.dto.request.EstadoPedidoRequest;
import com.pedidos360.bff.dto.request.PedidoRequest;
import com.pedidos360.bff.dto.response.PedidoResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente tipado para el microservicio de pedidos (pedidos360-pedidos-service).
 */
@Component
public class PedidoServiceClient {

    private final RestClient restClient;

    public PedidoServiceClient(@Qualifier("pedidosRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<PedidoResponse> findAll() {
        return restClient.get()
                .uri("/api/pedidos")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public PedidoResponse findById(Long id) {
        return restClient.get()
                .uri("/api/pedidos/{id}", id)
                .retrieve()
                .body(PedidoResponse.class);
    }

    public PedidoResponse create(PedidoRequest req) {
        return restClient.post()
                .uri("/api/pedidos")
                .body(req)
                .retrieve()
                .body(PedidoResponse.class);
    }

    public PedidoResponse updateEstado(Long id, EstadoPedidoRequest req) {
        return restClient.patch()
                .uri("/api/pedidos/{id}/estado", id)
                .body(req)
                .retrieve()
                .body(PedidoResponse.class);
    }

    public void delete(Long id) {
        restClient.delete()
                .uri("/api/pedidos/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
