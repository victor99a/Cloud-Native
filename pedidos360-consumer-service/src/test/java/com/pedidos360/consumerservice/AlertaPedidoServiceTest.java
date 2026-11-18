package com.pedidos360.consumerservice;

import com.pedidos360.consumerservice.messaging.NonRetryableException;
import com.pedidos360.consumerservice.streaming.config.KafkaStreamingProperties;
import com.pedidos360.consumerservice.streaming.dto.AlertaPedido.TipoAlerta;
import com.pedidos360.consumerservice.streaming.dto.PedidoEventDto;
import com.pedidos360.consumerservice.streaming.service.AlertaPedidoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de la lógica de alertas en tiempo real (sin Kafka).
 */
class AlertaPedidoServiceTest {

    private final AlertaPedidoService service = new AlertaPedidoService(
            new KafkaStreamingProperties("topic", "grupo", new BigDecimal("100000")));

    @Test
    void generaAlertaDeAltoValor() {
        var alertas = service.evaluar(evento("PENDIENTE", "150000"));

        assertThat(alertas).extracting(a -> a.tipo()).containsExactly(TipoAlerta.ALTO_VALOR);
    }

    @Test
    void generaAlertaDeCancelacion() {
        var alertas = service.evaluar(evento("CANCELADO", "5000"));

        assertThat(alertas).extracting(a -> a.tipo()).containsExactly(TipoAlerta.CANCELADO);
    }

    @Test
    void pedidoNormalNoGeneraAlertas() {
        assertThat(service.evaluar(evento("CONFIRMADO", "5000"))).isEmpty();
    }

    @Test
    void eventoSinEstadoEsIrrecuperable() {
        assertThatThrownBy(() -> service.evaluar(evento(" ", "5000")))
                .isInstanceOf(NonRetryableException.class);
    }

    private static PedidoEventDto evento(String estado, String total) {
        return new PedidoEventDto(1L, "PED-000001", "Cliente A", estado, new BigDecimal(total), LocalDateTime.now());
    }
}
