package com.pedidos360.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Decodificador/validador de JWT del Gateway.
 *
 * <p>Usa el {@code jwk-set-uri} interno (keycloak:8080) para obtener las claves
 * públicas sin depender del hostname que ve el navegador, y valida de forma
 * explícita el {@code issuer} (http://localhost:8090/realms/pedidos360) y el
 * {@code audience} (pedidos360-spa). Un token ausente, manipulado o expirado es
 * rechazado aquí, ANTES de reenviar la petición a los microservicios.
 */
@Configuration
@EnableConfigurationProperties(KeycloakJwtProperties.class)
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder keycloakJwtDecoder(KeycloakJwtProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(props.jwkSetUri()).build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(props.issuerUri());
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            if (jwt.getAudience() != null && jwt.getAudience().contains(props.audience())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "El audience del token no es válido", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
    }
}
