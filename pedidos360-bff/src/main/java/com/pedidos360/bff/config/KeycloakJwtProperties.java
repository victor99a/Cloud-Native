package com.pedidos360.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros de validación del JWT emitido por Keycloak (IDaaS local).
 * Centralizados en {@code application.yml} (prefijo {@code app.security.keycloak}).
 */
@ConfigurationProperties(prefix = "app.security.keycloak")
public record KeycloakJwtProperties(
        String issuerUri,
        String jwkSetUri,
        String audience
) {
}
