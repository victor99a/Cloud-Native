package com.pedidos360.analyticsservice.controller;

import com.pedidos360.analyticsservice.dto.MetricasPedidoDto;
import com.pedidos360.analyticsservice.service.AnalyticsService;
import com.pedidos360.analyticsservice.service.ReportingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de consulta de analítica. El frontend consume estos datos vía BFF →
 * API Gateway (que ya validan el JWT).
 */
@RestController
@RequestMapping("/api/analitica")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ReportingService reportingService;

    public AnalyticsController(AnalyticsService analyticsService, ReportingService reportingService) {
        this.analyticsService = analyticsService;
        this.reportingService = reportingService;
    }

    /** Resumen de métricas agregadas por estado (pedidos y monto vendido). */
    @GetMapping("/resumen")
    public List<MetricasPedidoDto> resumen() {
        return analyticsService.resumen();
    }

    /**
     * Snapshot del consumer group de reporting: último valor por estado leído
     * desde el tópico compactado de métricas.
     */
    @GetMapping("/reporting")
    public List<MetricasPedidoDto> reporting() {
        return reportingService.snapshot();
    }
}
