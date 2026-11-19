package com.pedidos360.analyticsservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pedidos360.analyticsservice.dto.PedidoEventDto;
import com.pedidos360.analyticsservice.entity.MetricaPedido;
import com.pedidos360.analyticsservice.entity.PedidoEstadoActual;
import com.pedidos360.analyticsservice.exception.NonRetryableEventException;
import com.pedidos360.analyticsservice.repository.MetricaPedidoRepository;
import com.pedidos360.analyticsservice.repository.PedidoEstadoActualRepository;
import com.pedidos360.analyticsservice.service.AnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la lógica de dominio de analítica: cada pedido debe contar UNA sola vez
 * (en su estado actual), los duplicados se ignoran y las eliminaciones descuentan.
 * Los repositorios se simulan con Mockito (sin infraestructura externa).
 */
class AnalyticsServiceTest {

    private static final BigDecimal TOTAL = new BigDecimal("150.00");

    private final MetricaPedidoRepository metricas = mock(MetricaPedidoRepository.class);
    private final PedidoEstadoActualRepository estados = mock(PedidoEstadoActualRepository.class);
    private final AnalyticsService service = new AnalyticsService(metricas, estados);

    @BeforeEach
    void setUp() {
        when(metricas.findByEstado(anyString())).thenAnswer(inv -> Optional.of(MetricaPedido.builder()
                .estado(inv.getArgument(0))
                .totalPedidos(1L)
                .totalVendido(TOTAL)
                .ultimaActualizacion(LocalDateTime.now())
                .build()));
    }

    private static PedidoEventDto evento(String eventId, String tipo, String estado) {
        return new PedidoEventDto(eventId, tipo, LocalDateTime.now(),
                1L, "PED-000001", "Cliente A", estado, TOTAL, LocalDateTime.now());
    }

    private static PedidoEstadoActual previo(String estado, String eventId) {
        return PedidoEstadoActual.builder()
                .pedidoId(1L).numero("PED-000001").estado(estado).total(TOTAL)
                .ultimoEventId(eventId).actualizadoEn(LocalDateTime.now()).build();
    }

    @Test
    void altaDePedido_sumaUnoEnSuEstado() {
        when(estados.findById(1L)).thenReturn(Optional.empty());

        var resultado = service.registrarEvento(evento("e1", "PEDIDO_CREADO", "PENDIENTE"));

        verify(metricas).ajustar(eq("PENDIENTE"), eq(1L), eq(TOTAL), any(LocalDateTime.class));
        verify(estados).save(any(PedidoEstadoActual.class));
        assertThat(resultado).extracting("estado").containsExactly("PENDIENTE");
    }

    @Test
    void cambioDeEstado_descuentaDelAnteriorYSumaAlNuevo() {
        PedidoEstadoActual actual = previo("PENDIENTE", "e1");
        when(estados.findById(1L)).thenReturn(Optional.of(actual));

        var resultado = service.registrarEvento(evento("e2", "ESTADO_ACTUALIZADO", "ENVIADO"));

        verify(metricas).ajustar(eq("PENDIENTE"), eq(-1L), eq(TOTAL.negate()), any(LocalDateTime.class));
        verify(metricas).ajustar(eq("ENVIADO"), eq(1L), eq(TOTAL), any(LocalDateTime.class));
        assertThat(actual.getEstado()).isEqualTo("ENVIADO");
        assertThat(actual.getUltimoEventId()).isEqualTo("e2");
        assertThat(resultado).extracting("estado").containsExactly("PENDIENTE", "ENVIADO");
    }

    @Test
    void eventoDuplicado_noModificaMetricas() {
        when(estados.findById(1L)).thenReturn(Optional.of(previo("ENVIADO", "e2")));

        var resultado = service.registrarEvento(evento("e2", "ESTADO_ACTUALIZADO", "ENVIADO"));

        verify(metricas, never()).ajustar(anyString(), anyLong(), any(), any());
        assertThat(resultado).isEmpty();
    }

    @Test
    void eliminacion_descuentaDelUltimoEstadoYBorraElSeguimiento() {
        PedidoEstadoActual actual = previo("ENVIADO", "e2");
        when(estados.findById(1L)).thenReturn(Optional.of(actual));

        var resultado = service.registrarEvento(evento("e3", "PEDIDO_ELIMINADO", "ENVIADO"));

        verify(metricas).ajustar(eq("ENVIADO"), eq(-1L), eq(TOTAL.negate()), any(LocalDateTime.class));
        verify(estados).delete(actual);
        assertThat(resultado).extracting("estado").containsExactly("ENVIADO");
    }

    @Test
    void resumen_ocultaEstadosSinPedidos() {
        when(metricas.findAllByOrderByEstadoAsc()).thenReturn(List.of(
                MetricaPedido.builder().estado("ENVIADO").totalPedidos(1L).totalVendido(TOTAL)
                        .ultimaActualizacion(LocalDateTime.now()).build(),
                MetricaPedido.builder().estado("PENDIENTE").totalPedidos(0L).totalVendido(BigDecimal.ZERO)
                        .ultimaActualizacion(LocalDateTime.now()).build()));

        assertThat(service.resumen()).extracting("estado").containsExactly("ENVIADO");
    }

    @Test
    void rechazaEventoConEstadoVacio() {
        assertThatThrownBy(() -> service.registrarEvento(evento("e1", "PEDIDO_CREADO", "")))
                .isInstanceOf(NonRetryableEventException.class)
                .hasMessageContaining("estado");
    }

    @Test
    void rechazaTipoDeEventoDesconocido() {
        assertThatThrownBy(() -> service.registrarEvento(evento("e1", "OTRO", "PENDIENTE")))
                .isInstanceOf(NonRetryableEventException.class)
                .hasMessageContaining("tipoEvento");
    }

    @Test
    void rechazaEventoNulo() {
        assertThatThrownBy(() -> service.registrarEvento(null))
                .isInstanceOf(NonRetryableEventException.class);
    }
}
