package com.pedidos360.bff.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedidos360.bff.client.AnalyticsServiceClient;
import com.pedidos360.bff.dto.response.MetricaAnaliticaResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rutas de analítica del BFF: resumen (PostgreSQL) y reporting (vista materializada
 * del tópico compactado). Una ruta inexistente responde 404, no 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BffAnaliticaTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsServiceClient analyticsServiceClient;

    @Test
    void reportingDevuelveLaVistaDelTopicoCompactado() throws Exception {
        when(analyticsServiceClient.reporting()).thenReturn(List.of(
                new MetricaAnaliticaResponse("ENVIADO", 1L, new BigDecimal("1700000.00"), LocalDateTime.now())));

        mockMvc.perform(get("/api/analitica/reporting")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("ENVIADO"))
                .andExpect(jsonPath("$[0].totalPedidos").value(1));
    }

    @Test
    void reportingSinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/analitica/reporting"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/analitica/no-existe")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("test-user")
                    .build();
        }
    }
}
