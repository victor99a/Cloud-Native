package com.pedidos360.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

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
    public ReactiveJwtDecoder keycloakJwtDecoder(KeycloakJwtProperties props) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(props.jwkSetUri()).build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(props.issuerUri());
        // Keycloak emite el cliente en "aud"; Amazon Cognito (access token) en "client_id".
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            boolean audOk = jwt.getAudience() != null && jwt.getAudience().contains(props.audience());
            boolean clientIdOk = props.audience().equals(jwt.getClaimAsString("client_id"));
            if (audOk || clientIdOk) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "El audience del token no es válido", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerValidator, audienceValidator, new AccessTokenValidator()));
        return decoder;
    }
}
