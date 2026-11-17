package com.pedidos360.analyticsservice.service;

import com.pedidos360.analyticsservice.dto.MetricasPedidoDto;
import com.pedidos360.analyticsservice.dto.PedidoEventDto;
import com.pedidos360.analyticsservice.entity.MetricaPedido;
import com.pedidos360.analyticsservice.exception.NonRetryableEventException;
import com.pedidos360.analyticsservice.repository.MetricaPedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de dominio de analítica: valida el evento, agrega la métrica por estado
 * en la base de datos y expone el resumen para la API REST.
 */
@Slf4j
@Service
public class AnalyticsService {

    private final MetricaPedidoRepository repository;

    public AnalyticsService(MetricaPedidoRepository repository) {
        this.repository = repository;
    }

    /**
     * Valida y registra un evento de pedido. Devuelve la métrica actualizada del
     * estado para publicarla en el tópico de métricas.
     */
    @Transactional
    public MetricasPedidoDto registrarEvento(PedidoEventDto evento) {
        validar(evento);

        LocalDateTime ahora = LocalDateTime.now();
        repository.increment(evento.estado(), evento.total(), ahora);

        MetricaPedido metrica = repository.findByEstado(evento.estado())
                .orElseThrow(() -> new IllegalStateException(
                        "Métrica no encontrada tras el upsert para estado=" + evento.estado()));

        log.info("[analitica] evento registrado pedidoId={} numero={} estado={} total={}",
                evento.pedidoId(), evento.numero(), evento.estado(), evento.total());

        return new MetricasPedidoDto(
                metrica.getEstado(),
                metrica.getTotalPedidos(),
                metrica.getTotalVendido(),
                metrica.getUltimaActualizacion());
    }

    /** Resumen de métricas para el dashboard (consultado vía BFF). */
    @Transactional(readOnly = true)
    public List<MetricasPedidoDto> resumen() {
        return repository.findAllByOrderByEstadoAsc().stream()
                .map(m -> new MetricasPedidoDto(
                        m.getEstado(),
                        m.getTotalPedidos(),
                        m.getTotalVendido(),
                        m.getUltimaActualizacion()))
                .toList();
    }

    private void validar(PedidoEventDto evento) {
        if (evento == null) {
            throw new NonRetryableEventException("evento nulo");
        }
        if (evento.pedidoId() == null) {
            throw new NonRetryableEventException("pedidoId es obligatorio");
        }
        if (evento.estado() == null || evento.estado().isBlank()) {
            throw new NonRetryableEventException("estado es obligatorio");
        }
        if (evento.total() == null || evento.total().compareTo(BigDecimal.ZERO) < 0) {
            throw new NonRetryableEventException("total inválido: " + evento.total());
        }
    }
}
