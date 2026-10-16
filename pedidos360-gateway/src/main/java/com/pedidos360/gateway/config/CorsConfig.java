package com.pedidos360.gateway.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS del Gateway. Como el frontend (http://localhost:4200) consume el Gateway
 * (http://localhost:8080) desde otro origen, este filtro responde los preflight
 * OPTIONS. En un entorno productivo se restringirían los orígenes permitidos.
 *
 * <p>Se ordena con {@link Ordered#HIGHEST_PRECEDENCE} para que el preflight OPTIONS
 * se resuelva ANTES del filtro de seguridad reactiva ({@code WebFilterChainProxy});
 * de lo contrario, el preflight (sin token) sería rechazado con HTTP 401.
 */
@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
