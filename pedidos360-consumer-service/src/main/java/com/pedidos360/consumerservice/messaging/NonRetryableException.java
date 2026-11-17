package com.pedidos360.consumerservice.messaging;

/**
 * Marca un fallo IRRECUPERABLE ("poison message"): el mensaje nunca podrá procesarse
 * correctamente (payload vacío, formato inválido, violación de regla de negocio).
 *
 * <p>Los consumidores la usan para decidir la ruta de error: ante esta excepción el
 * mensaje se desvía a la DLQ ({@code basicNack(requeue=false)}); ante cualquier otra
 * excepción (error transitorio) se reintenta ({@code basicNack(requeue=true)}).
 */
public class NonRetryableException extends RuntimeException {

    public NonRetryableException(String message) {
        super(message);
    }

    public NonRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
