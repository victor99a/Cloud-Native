package com.pedidos360.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Seguridad reactiva del Gateway.
 *
 * <p>Valida el JWT (OAuth2 Resource Server) ANTES de enrutar hacia los
 * microservicios: una petición sin token, con token manipulado o expirado es
 * rechazada aquí con HTTP 401. Solo {@code /api/health} y {@code /actuator/**}
 * quedan públicos para el healthcheck de Docker Compose.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/health", "/actuator/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oAuth2 -> oAuth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
