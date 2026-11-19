package com.pedidos360.analyticsservice.service;

import com.pedidos360.analyticsservice.dto.MetricasPedidoDto;
import com.pedidos360.analyticsservice.dto.PedidoEventDto;
import com.pedidos360.analyticsservice.entity.MetricaPedido;
import com.pedidos360.analyticsservice.entity.PedidoEstadoActual;
import com.pedidos360.analyticsservice.exception.NonRetryableEventException;
import com.pedidos360.analyticsservice.repository.MetricaPedidoRepository;
import com.pedidos360.analyticsservice.repository.PedidoEstadoActualRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de dominio de analítica.
 *
 * <p>Mantiene, por estado, cuántos pedidos hay ACTUALMENTE en ese estado y su monto.
 * Para que cada pedido cuente una sola vez se materializa su último estado
 * ({@link PedidoEstadoActual}):
 * <ul>
 *   <li>Alta: +1 en el estado del evento.</li>
 *   <li>Cambio de estado: -1 en el estado anterior y +1 en el nuevo.</li>
 *   <li>Eliminación: -1 en el último estado conocido.</li>
 *   <li>Evento repetido (mismo {@code eventId}) o sin cambios: se ignora (idempotencia).</li>
 * </ul>
 * Los eventos de un mismo pedido llegan en orden porque la clave Kafka es su número
 * (misma partición), así que no hay carreras sobre el mismo pedido.
 */
@Slf4j
@Service
public class AnalyticsService {

    private final MetricaPedidoRepository metricaRepository;
    private final PedidoEstadoActualRepository estadoRepository;

    public AnalyticsService(MetricaPedidoRepository metricaRepository,
                            PedidoEstadoActualRepository estadoRepository) {
        this.metricaRepository = metricaRepository;
        this.estadoRepository = estadoRepository;
    }

    /**
     * Valida y aplica un evento de pedido. Devuelve las métricas de los estados
     * afectados (para publicarlas en el tópico de métricas); lista vacía si el
     * evento era un duplicado o no cambiaba nada.
     */
    @Transactional
    public List<MetricasPedidoDto> registrarEvento(PedidoEventDto evento) {
        validar(evento);
        LocalDateTime ahora = LocalDateTime.now();
        Optional<PedidoEstadoActual> previo = estadoRepository.findById(evento.pedidoId());

        if (previo.isPresent() && evento.eventId() != null
                && evento.eventId().equals(previo.get().getUltimoEventId())) {
            log.info("[analitica] evento duplicado ignorado eventId={} pedido={}",
                    evento.eventId(), evento.numero());
            return List.of();
        }

        Set<String> afectados = new LinkedHashSet<>();

        if (evento.esEliminacion()) {
            previo.ifPresent(p -> {
                ajustar(p.getEstado(), -1, p.getTotal().negate(), ahora, afectados);
                estadoRepository.delete(p);
            });
            log.info("[analitica] pedido eliminado numero={} estadoPrevio={}",
                    evento.numero(), previo.map(PedidoEstadoActual::getEstado).orElse("desconocido"));
            return metricasDe(afectados);
        }

        if (previo.isEmpty()) {
            ajustar(evento.estado(), 1, evento.total(), ahora, afectados);
            estadoRepository.save(PedidoEstadoActual.builder()
                    .pedidoId(evento.pedidoId())
                    .numero(evento.numero())
                    .estado(evento.estado())
                    .total(evento.total())
                    .ultimoEventId(evento.eventId())
                    .actualizadoEn(ahora)
                    .build());
        } else {
            PedidoEstadoActual actual = previo.get();
            boolean mismoEstado = actual.getEstado().equals(evento.estado());
            boolean mismoTotal = actual.getTotal().compareTo(evento.total()) == 0;
            if (mismoEstado && mismoTotal) {
                log.info("[analitica] evento sin cambios ignorado pedido={} estado={}",
                        evento.numero(), evento.estado());
                actual.setUltimoEventId(evento.eventId());
                return List.of();
            }
            if (mismoEstado) {
                ajustar(evento.estado(), 0, evento.total().subtract(actual.getTotal()), ahora, afectados);
            } else {
                ajustar(actual.getEstado(), -1, actual.getTotal().negate(), ahora, afectados);
                ajustar(evento.estado(), 1, evento.total(), ahora, afectados);
            }
            actual.setEstado(evento.estado());
            actual.setTotal(evento.total());
            actual.setUltimoEventId(evento.eventId());
            actual.setActualizadoEn(ahora);
        }

        log.info("[analitica] evento aplicado pedidoId={} numero={} tipo={} estado={} total={} afectados={}",
                evento.pedidoId(), evento.numero(), evento.tipoEvento(), evento.estado(),
                evento.total(), afectados);
        return metricasDe(afectados);
    }

    /** Resumen de métricas para el dashboard (consultado vía BFF): solo estados con pedidos. */
    @Transactional(readOnly = true)
    public List<MetricasPedidoDto> resumen() {
        return metricaRepository.findAllByOrderByEstadoAsc().stream()
                .filter(m -> m.getTotalPedidos() > 0)
                .map(this::toDto)
                .toList();
    }

    private void ajustar(String estado, long deltaPedidos, BigDecimal deltaTotal,
                         LocalDateTime fecha, Set<String> afectados) {
        metricaRepository.ajustar(estado, deltaPedidos, deltaTotal, fecha);
        afectados.add(estado);
    }

    private List<MetricasPedidoDto> metricasDe(Set<String> estados) {
        return estados.stream()
                .map(metricaRepository::findByEstado)
                .flatMap(Optional::stream)
                .map(this::toDto)
                .toList();
    }

    private MetricasPedidoDto toDto(MetricaPedido m) {
        return new MetricasPedidoDto(
                m.getEstado(), m.getTotalPedidos(), m.getTotalVendido(), m.getUltimaActualizacion());
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
        if (evento.tipoEvento() != null && !Objects.equals(evento.tipoEvento(), "PEDIDO_CREADO")
                && !Objects.equals(evento.tipoEvento(), "ESTADO_ACTUALIZADO")
                && !evento.esEliminacion()) {
            throw new NonRetryableEventException("tipoEvento desconocido: " + evento.tipoEvento());
        }
    }
}
