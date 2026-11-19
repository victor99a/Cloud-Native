package com.pedidos360.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Pruebas del perímetro de seguridad del API Gateway: las rutas de negocio
 * exigen un JWT válido y el healthcheck queda público. No requiere Keycloak:
 * la petición se rechaza antes de consultar el JWKS o enrutar al BFF.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void peticionSinTokenEsRechazadaCon401() {
        webTestClient.get().uri("/api/pedidos")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void tokenMalformadoEsRechazadoCon401() {
        webTestClient.get().uri("/api/analitica/resumen")
                .header(HttpHeaders.AUTHORIZATION, "Bearer esto-no-es-un-jwt")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void healthcheckEsPublico() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
}
