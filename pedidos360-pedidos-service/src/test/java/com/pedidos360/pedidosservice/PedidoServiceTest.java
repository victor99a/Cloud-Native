package com.pedidos360.pedidosservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.pedidosservice.dto.request.PedidoRequest;
import com.pedidos360.pedidosservice.entity.Producto;
import com.pedidos360.pedidosservice.repository.ProductoRepository;
import java.math.BigDecimal;
import java.util.List;
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
class PedidoServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void createConDosItems_calculaSubtotalYTotal() throws Exception {
        Producto a = productoRepository.save(Producto.builder()
                .sku("SKU-A")
                .nombre("Producto A")
                .precio(new BigDecimal("50.00"))
                .build());
        Producto b = productoRepository.save(Producto.builder()
                .sku("SKU-B")
                .nombre("Producto B")
                .precio(new BigDecimal("25.00"))
                .build());

        PedidoRequest pedido = new PedidoRequest("Juan Perez",
                List.of(
                        new PedidoRequest.PedidoItemRequest(a.getId(), 2),
                        new PedidoRequest.PedidoItemRequest(b.getId(), 1)
                ));

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(100.00))
                .andExpect(jsonPath("$.items[1].cantidad").value(1))
                .andExpect(jsonPath("$.items[1].subtotal").value(25.00))
                .andExpect(jsonPath("$.total").value(125.00));
    }

    @Test
    void updateEstado_actualizaEstado() throws Exception {
        Producto a = productoRepository.save(Producto.builder()
                .sku("SKU-ESTADO")
                .nombre("Producto Estado")
                .precio(new BigDecimal("10.00"))
                .build());

        PedidoRequest pedido = new PedidoRequest("Cliente X",
                List.of(new PedidoRequest.PedidoItemRequest(a.getId(), 1)));

        MvcResult created = mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedido)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/pedidos/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ENVIADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADO"));
    }

    @Test
    void createConProductoInexistente_retorna404() throws Exception {
        PedidoRequest pedido = new PedidoRequest("Cliente X",
                List.of(new PedidoRequest.PedidoItemRequest(999999L, 1)));

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedido)))
                .andExpect(status().isNotFound());
    }
}
