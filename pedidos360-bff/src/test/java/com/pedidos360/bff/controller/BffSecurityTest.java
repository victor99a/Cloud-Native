package com.pedidos360.bff.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BffSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpoint_esPublico_sinToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void rutaProxied_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mutacionProducto_sinRolStaff_retorna403() throws Exception {
        // Un CLIENTE (sin rol ADMIN/OPERADOR) no puede crear productos
        mockMvc.perform(post("/api/productos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-X\",\"nombre\":\"X\",\"precio\":10,\"stock\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mutacionPedido_sinRolStaff_retorna403() throws Exception {
        // Un CLIENTE no puede eliminar pedidos
        mockMvc.perform(delete("/api/pedidos/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void lectura_conRolCliente_pasaAutorizacion() throws Exception {
        // La lectura sí está permitida para cualquier usuario autenticado.
        // Como el BFF es un proxy sin downstream en el test, se espera 5xx (no 401/403).
        mockMvc.perform(get("/api/productos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().is5xxServerError());
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
