package com.pedidos360.analyticsservice.repository;

import com.pedidos360.analyticsservice.entity.MetricaPedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MetricaPedidoRepository extends JpaRepository<MetricaPedido, Long> {

    List<MetricaPedido> findAllByOrderByEstadoAsc();

    Optional<MetricaPedido> findByEstado(String estado);

    /**
     * Ajuste atómico (upsert) de la métrica de un estado sumando deltas (+1/-1 pedidos,
     * +/- monto). Con varios consumidores repartiéndose las particiones, la
     * concurrencia se resuelve con {@code ON CONFLICT (estado)} en PostgreSQL en lugar
     * de leer-modificar-escribir en la JVM.
     */
    @Modifying
    @Query(value = """
            INSERT INTO metrica_pedido (estado, total_pedidos, total_vendido, ultima_actualizacion)
            VALUES (:estado, :deltaPedidos, :deltaTotal, :fecha)
            ON CONFLICT (estado) DO UPDATE
            SET total_pedidos = metrica_pedido.total_pedidos + EXCLUDED.total_pedidos,
                total_vendido = metrica_pedido.total_vendido + EXCLUDED.total_vendido,
                ultima_actualizacion = EXCLUDED.ultima_actualizacion
            """, nativeQuery = true)
    void ajustar(@Param("estado") String estado,
                 @Param("deltaPedidos") long deltaPedidos,
                 @Param("deltaTotal") BigDecimal deltaTotal,
                 @Param("fecha") LocalDateTime fecha);
}
