package com.pedidos360.bff.config;

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
 * Decodificador/validador de JWT del BFF (defensa en profundidad).
 *
 * <p>Obtiene las claves públicas desde el {@code jwk-set-uri} interno
 * (keycloak:8080) y valida explícitamente el {@code issuer} y el {@code audience}.
 * El Gateway ya rechaza tokens inválidos; el BFF repite la validación para que
 * ningún microservicio confíe únicamente en la perimetral.
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
