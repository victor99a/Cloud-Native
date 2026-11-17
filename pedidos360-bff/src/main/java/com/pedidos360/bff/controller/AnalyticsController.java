package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.AnalyticsServiceClient;
import com.pedidos360.bff.dto.response.MetricaAnaliticaResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone la analítica de pedidos al frontend. Protegida por JWT (cae bajo
 * {@code anyRequest().authenticated()} del SecurityConfig).
 */
@RestController
@RequestMapping("/api/analitica")
public class AnalyticsController {

    private final AnalyticsServiceClient analyticsServiceClient;

    public AnalyticsController(AnalyticsServiceClient analyticsServiceClient) {
        this.analyticsServiceClient = analyticsServiceClient;
    }

    @GetMapping("/resumen")
    @PreAuthorize("isAuthenticated()")
    public List<MetricaAnaliticaResponse> resumen() {
        return analyticsServiceClient.resumen();
    }
}
