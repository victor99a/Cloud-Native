package com.pedidos360.analyticsservice.exception;

/**
 * Error irrecuperable de un evento consumido: el payload es inválido y nunca
 * podrá procesarse (poison message). El {@code DefaultErrorHandler} lo desvía
 * directamente al Dead Letter Topic sin reintentos.
 */
public class NonRetryableEventException extends RuntimeException {

    public NonRetryableEventException(String message) {
        super(message);
    }
}
