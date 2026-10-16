package com.pedidos360.productosservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.productosservice.dto.request.ProductoRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void productoCicloCompleto_createFindUpdateDelete() throws Exception {
        ProductoRequest create = new ProductoRequest(
                "SKU-TEST-1", "Notebook", "Notebook 16GB", new BigDecimal("999.99"), 10, true);

        MvcResult created = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-TEST-1"))
                .andExpect(jsonPath("$.nombre").value("Notebook"))
                .andExpect(jsonPath("$.stock").value(10))
                .andReturn();

        Long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // findById
        mockMvc.perform(get("/api/productos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        // update
        ProductoRequest update = new ProductoRequest(
                "SKU-TEST-1", "Notebook Pro", "Notebook 32GB", new BigDecimal("1299.99"), 5, true);
        mockMvc.perform(put("/api/productos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Notebook Pro"))
                .andExpect(jsonPath("$.stock").value(5));

        // delete
        mockMvc.perform(delete("/api/productos/{id}", id))
                .andExpect(status().isNoContent());

        // find after delete -> 404
        mockMvc.perform(get("/api/productos/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void productoInvalido_retorna400() throws Exception {
        ProductoRequest invalid = new ProductoRequest("", "", null, null, null, true);
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}
