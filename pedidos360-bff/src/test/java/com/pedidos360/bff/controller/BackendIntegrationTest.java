package com.pedidos360.bff.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.bff.dto.request.PedidoRequest;
import com.pedidos360.bff.dto.request.ProductoRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthEndpoint_esPublico_sinToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void accesoSinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productoCicloCompleto_createFindUpdateDelete() throws Exception {
        ProductoRequest create = new ProductoRequest(
                "SKU-TEST-1", "Notebook", "Notebook 16GB", new BigDecimal("999.99"), 10, true);

        MvcResult created = mockMvc.perform(post("/api/productos")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-TEST-1"))
                .andExpect(jsonPath("$.nombre").value("Notebook"))
                .andExpect(jsonPath("$.stock").value(10))
                .andReturn();

        Long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // findById
        mockMvc.perform(get("/api/productos/{id}", id).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        // update
        ProductoRequest update = new ProductoRequest(
                "SKU-TEST-1", "Notebook Pro", "Notebook 32GB", new BigDecimal("1299.99"), 5, true);
        mockMvc.perform(put("/api/productos/{id}", id)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Notebook Pro"))
                .andExpect(jsonPath("$.stock").value(5));

        // delete
        mockMvc.perform(delete("/api/productos/{id}", id).with(jwt()))
                .andExpect(status().isNoContent());

        // find after delete -> 404
        mockMvc.perform(get("/api/productos/{id}", id).with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void productoInvalido_retorna400() throws Exception {
        ProductoRequest invalid = new ProductoRequest("", "", null, null, null, true);
        mockMvc.perform(post("/api/productos")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pedidoCreacion_calculaTotalYActualizaEstado() throws Exception {
        // crear producto
        ProductoRequest producto = new ProductoRequest(
                "SKU-PED-1", "Teclado", "Teclado mecánico", new BigDecimal("50.00"), 20, true);
        MvcResult created = mockMvc.perform(post("/api/productos")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isCreated())
                .andReturn();
        Long productoId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // crear pedido con 2 unidades
        PedidoRequest pedido = new PedidoRequest("Juan Perez",
                List.of(new PedidoRequest.PedidoItemRequest(productoId, 2)));

        MvcResult pedidoResult = mockMvc.perform(post("/api/pedidos")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(100.00))
                .andExpect(jsonPath("$.total").value(100.00))
                .andReturn();

        Long pedidoId = objectMapper.readTree(pedidoResult.getResponse().getContentAsString()).get("id").asLong();

        // actualizar estado
        mockMvc.perform(patch("/api/pedidos/{id}/estado", pedidoId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ENVIADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADO"));
    }

    @Test
    void pedidoConProductoInexistente_retorna404() throws Exception {
        PedidoRequest pedido = new PedidoRequest("Cliente X",
                List.of(new PedidoRequest.PedidoItemRequest(999999L, 1)));
        mockMvc.perform(post("/api/pedidos")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedido)))
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
