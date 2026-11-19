package com.pedidos360.analyticsservice.service;

import com.pedidos360.analyticsservice.dto.MetricasPedidoDto;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Vista materializada del tópico compactado {@code pedidos360.analitica.metricas}.
 *
 * <p>Como el tópico usa {@code cleanup.policy=compact} con clave = estado, el
 * último valor por clave representa el estado vigente. Este servicio mantiene
 * ese "último valor por estado" en memoria para el consumer group de reporting,
 * descartando métricas desordenadas (más antiguas que la ya registrada).
 */
@Slf4j
@Service
public class ReportingService {

    private final Map<String, MetricasPedidoDto> snapshot = new ConcurrentHashMap<>();

    /** Aplica una métrica; devuelve {@code true} si actualizó el snapshot. */
    public boolean aplicar(MetricasPedidoDto metrica) {
        if (metrica == null || metrica.estado() == null || metrica.estado().isBlank()) {
            log.warn("[reporting] métrica descartada por inválida: {}", metrica);
            return false;
        }
        MetricasPedidoDto resultado = snapshot.merge(metrica.estado(), metrica, (actual, nueva) ->
                esMasReciente(nueva, actual) ? nueva : actual);
        return resultado == metrica;
    }

    public List<MetricasPedidoDto> snapshot() {
        return snapshot.values().stream()
                .sorted(Comparator.comparing(MetricasPedidoDto::estado))
                .toList();
    }

    private static boolean esMasReciente(MetricasPedidoDto nueva, MetricasPedidoDto actual) {
        if (nueva.ultimaActualizacion() == null || actual.ultimaActualizacion() == null) {
            return nueva.totalPedidos() >= actual.totalPedidos();
        }
        return !nueva.ultimaActualizacion().isBefore(actual.ultimaActualizacion());
    }
}
