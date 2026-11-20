package com.pedidos360.analyticsservice;

import com.pedidos360.analyticsservice.dto.MetricasPedidoDto;
import com.pedidos360.analyticsservice.service.ReportingService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de la vista materializada del consumer group de reporting.
 */
class ReportingServiceTest {

    private final ReportingService service = new ReportingService();

    @Test
    void conservaElUltimoValorPorEstado() {
        LocalDateTime t0 = LocalDateTime.of(2026, 1, 1, 10, 0);
        service.aplicar(new MetricasPedidoDto("PENDIENTE", 1, new BigDecimal("10"), t0));
        service.aplicar(new MetricasPedidoDto("PENDIENTE", 2, new BigDecimal("30"), t0.plusSeconds(5)));

        assertThat(service.snapshot()).singleElement()
                .satisfies(m -> assertThat(m.totalPedidos()).isEqualTo(2));
    }

    @Test
    void descartaMetricaDesordenada() {
        LocalDateTime t0 = LocalDateTime.of(2026, 1, 1, 10, 0);
        service.aplicar(new MetricasPedidoDto("ENTREGADO", 5, new BigDecimal("50"), t0));

        boolean actualizado = service.aplicar(
                new MetricasPedidoDto("ENTREGADO", 4, new BigDecimal("40"), t0.minusSeconds(5)));

        assertThat(actualizado).isFalse();
        assertThat(service.snapshot().get(0).totalPedidos()).isEqualTo(5);
    }

    @Test
    void ignoraMetricaSinEstado() {
        assertThat(service.aplicar(new MetricasPedidoDto(null, 1, BigDecimal.ONE, LocalDateTime.now()))).isFalse();
        assertThat(service.snapshot()).isEmpty();
    }

    @Test
    void ocultaEstadosQueQuedaronSinPedidos() {
        LocalDateTime t0 = LocalDateTime.now();
        service.aplicar(new MetricasPedidoDto("PENDIENTE", 1L, new BigDecimal("10.00"), t0));
        service.aplicar(new MetricasPedidoDto("PENDIENTE", 0L, BigDecimal.ZERO, t0.plusSeconds(1)));
        assertThat(service.snapshot()).isEmpty();
    }
}
