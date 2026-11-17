package com.pedidos360.bff.client;

import com.pedidos360.bff.dto.response.MetricaAnaliticaResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente tipado para el microservicio de analítica (pedidos360-analytics-service).
 */
@Component
public class AnalyticsServiceClient {

    private final RestClient restClient;

    public AnalyticsServiceClient(@Qualifier("analyticsRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<MetricaAnaliticaResponse> resumen() {
        return restClient.get()
                .uri("/api/analitica/resumen")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
