package com.pedidos360.consumerservice.streaming.service;

import com.pedidos360.consumerservice.messaging.NonRetryableException;
import com.pedidos360.consumerservice.streaming.config.KafkaStreamingProperties;
import com.pedidos360.consumerservice.streaming.dto.AlertaPedido;
import com.pedidos360.consumerservice.streaming.dto.AlertaPedido.TipoAlerta;
import com.pedidos360.consumerservice.streaming.dto.PedidoEventDto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Lógica de dominio de alertas en tiempo real: evalúa cada evento de pedido y
 * genera las alertas que correspondan. Independiente de Kafka (testeable).
 */
@Slf4j
@Service
public class AlertaPedidoService {

    private final BigDecimal umbralAltoValor;

    public AlertaPedidoService(KafkaStreamingProperties props) {
        this.umbralAltoValor = props.umbralAltoValor() != null
                ? props.umbralAltoValor()
                : new BigDecimal("100000");
    }

    public List<AlertaPedido> evaluar(PedidoEventDto evento) {
        validar(evento);

        List<AlertaPedido> alertas = new ArrayList<>();
        if (evento.total().compareTo(umbralAltoValor) >= 0) {
            alertas.add(new AlertaPedido(TipoAlerta.ALTO_VALOR, evento.numero(),
                    "Pedido de alto valor (" + evento.total() + " >= " + umbralAltoValor + ")"));
        }
        if ("CANCELADO".equals(evento.estado())) {
            alertas.add(new AlertaPedido(TipoAlerta.CANCELADO, evento.numero(),
                    "Pedido cancelado por/para " + evento.cliente()));
        }
        if ("ENTREGADO".equals(evento.estado())) {
            alertas.add(new AlertaPedido(TipoAlerta.ENTREGADO, evento.numero(),
                    "Pedido entregado: enviar encuesta de satisfacción a " + evento.cliente()));
        }

        alertas.forEach(a -> log.warn("[alertas] ALERTA tipo={} pedido={} detalle={}",
                a.tipo(), a.numeroPedido(), a.mensaje()));
        return alertas;
    }

    private void validar(PedidoEventDto evento) {
        if (evento == null) {
            throw new NonRetryableException("evento nulo");
        }
        if (evento.numero() == null || evento.numero().isBlank()) {
            throw new NonRetryableException("numero de pedido obligatorio");
        }
        if (evento.estado() == null || evento.estado().isBlank()) {
            throw new NonRetryableException("estado obligatorio");
        }
        if (evento.total() == null || evento.total().signum() < 0) {
            throw new NonRetryableException("total inválido: " + evento.total());
        }
    }
}
