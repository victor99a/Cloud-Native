package com.pedidos360.analyticsservice;

import com.pedidos360.analyticsservice.dto.PedidoEventDto;
import com.pedidos360.analyticsservice.entity.MetricaPedido;
import com.pedidos360.analyticsservice.exception.NonRetryableEventException;
import com.pedidos360.analyticsservice.repository.MetricaPedidoRepository;
import com.pedidos360.analyticsservice.service.AnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas básicas de la lógica de dominio de analítica (validación + agregación).
 * No requiere infraestructura externa: el repositorio se simula con Mockito.
 */
class AnalyticsServiceTest {

    private final MetricaPedidoRepository repository = mock(MetricaPedidoRepository.class);
    private final AnalyticsService service = new AnalyticsService(repository);

    @Test
    void registraEventoValidoEIncrementaMetrica() {
        PedidoEventDto evento = new PedidoEventDto(
                1L, "PED-000001", "Cliente A", "PENDIENTE", new BigDecimal("150.00"), LocalDateTime.now());

        MetricaPedido metrica = MetricaPedido.builder()
                .estado("PENDIENTE")
                .totalPedidos(1L)
                .totalVendido(new BigDecimal("150.00"))
                .ultimaActualizacion(LocalDateTime.now())
                .build();
        when(repository.findByEstado("PENDIENTE")).thenReturn(Optional.of(metrica));

        var resultado = service.registrarEvento(evento);

        verify(repository).increment(eq("PENDIENTE"), eq(new BigDecimal("150.00")), any(LocalDateTime.class));
        assertThat(resultado.estado()).isEqualTo("PENDIENTE");
        assertThat(resultado.totalPedidos()).isEqualTo(1L);
    }

    @Test
    void rechazaEventoConEstadoVacio() {
        PedidoEventDto evento = new PedidoEventDto(1L, "PED-000001", "Cliente A", "", BigDecimal.ZERO, LocalDateTime.now());

        assertThatThrownBy(() -> service.registrarEvento(evento))
                .isInstanceOf(NonRetryableEventException.class)
                .hasMessageContaining("estado");
    }

    @Test
    void rechazaEventoNulo() {
        assertThatThrownBy(() -> service.registrarEvento(null))
                .isInstanceOf(NonRetryableEventException.class);
    }
}
