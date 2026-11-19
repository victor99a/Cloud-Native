package com.pedidos360.analyticsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Último estado conocido de cada pedido según el stream de eventos.
 *
 * <p>Es el "estado materializado" que permite que las métricas cuenten cada pedido
 * UNA sola vez: ante un cambio de estado se descuenta del estado anterior y se suma
 * al nuevo. {@code ultimoEventId} hace el procesamiento idempotente (un evento
 * reentregado por un reintento o rebalanceo no vuelve a sumar).
 */
@Entity
@Table(name = "pedido_estado_actual")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEstadoActual {

    @Id
    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "numero", length = 32)
    private String numero;

    @Column(name = "estado", nullable = false, length = 32)
    private String estado;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "ultimo_event_id", length = 64)
    private String ultimoEventId;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
