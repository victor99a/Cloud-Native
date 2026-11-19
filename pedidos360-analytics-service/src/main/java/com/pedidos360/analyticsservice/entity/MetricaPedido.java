package com.pedidos360.analyticsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Métrica agregada de pedidos persistida en PostgreSQL (base de datos cloud).
 * Una fila por estado: total de pedidos y monto vendido acumulados.
 */
@Entity
@Table(name = "metrica_pedido")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricaPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Estado del pedido (PENDIENTE, ENTREGADO, ...). Único: una fila por estado. */
    @Column(name = "estado", nullable = false, unique = true, length = 32)
    private String estado;

    @Column(name = "total_pedidos", nullable = false)
    private long totalPedidos;

    @Column(name = "total_vendido", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalVendido;

    @Column(name = "ultima_actualizacion", nullable = false)
    private LocalDateTime ultimaActualizacion;
}
