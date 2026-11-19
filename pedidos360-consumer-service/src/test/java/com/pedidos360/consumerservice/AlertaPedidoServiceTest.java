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
    void altoValorSoloSeAlertaAlCrear() {
        var alertas = service.evaluar(evento("ESTADO_ACTUALIZADO", "ENVIADO", "150000"));

        assertThat(alertas).isEmpty();
    }

    @Test
    void eliminacionNoGeneraAlertas() {
        assertThat(service.evaluar(evento("PEDIDO_ELIMINADO", "CANCELADO", "150000"))).isEmpty();
    }

    @Test
    void eventoSinEstadoEsIrrecuperable() {
        assertThatThrownBy(() -> service.evaluar(evento(" ", "5000")))
                .isInstanceOf(NonRetryableException.class);
    }

    private static PedidoEventDto evento(String estado, String total) {
        return evento("PEDIDO_CREADO", estado, total);
    }

    private static PedidoEventDto evento(String tipo, String estado, String total) {
        return new PedidoEventDto("e1", tipo, 1L, "PED-000001", "Cliente A", estado,
                new BigDecimal(total), LocalDateTime.now());
    }
}
