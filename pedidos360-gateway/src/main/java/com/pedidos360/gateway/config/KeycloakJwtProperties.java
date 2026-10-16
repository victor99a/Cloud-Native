package com.pedidos360.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros de validación del JWT emitido por Keycloak (IDaaS local).
 * Centralizados en {@code application.yml} (prefijo {@code app.security.keycloak}),
 * evitando hardcoding del issuer, del JWKS y del audience.
 */
@ConfigurationProperties(prefix = "app.security.keycloak")
public record KeycloakJwtProperties(
        String issuerUri,
        String jwkSetUri,
        String audience
) {
}
