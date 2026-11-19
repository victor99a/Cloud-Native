package com.pedidos360.gateway;

import com.pedidos360.gateway.config.OriginVerifyFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Con secreto de origen configurado (como en AWS): una petición directa al
 * gateway sin el encabezado del API Manager se rechaza con 403 antes de evaluar
 * el JWT; con el encabezado correcto pasa al control de JWT (401 sin token).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "pedidos360.origin-secret=secreto-de-prueba")
@AutoConfigureWebTestClient
class OriginVerifyFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void peticionDirectaSinEncabezadoEsRechazadaCon403() {
        webTestClient.get().uri("/api/pedidos")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void encabezadoIncorrectoEsRechazadoCon403() {
        webTestClient.get().uri("/api/pedidos")
                .header(OriginVerifyFilter.HEADER, "otro-valor")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void conEncabezadoDelApiManagerPasaAlControlDeJwt() {
        webTestClient.get().uri("/api/pedidos")
                .header(OriginVerifyFilter.HEADER, "secreto-de-prueba")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void healthcheckNoRequiereEncabezado() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
}
