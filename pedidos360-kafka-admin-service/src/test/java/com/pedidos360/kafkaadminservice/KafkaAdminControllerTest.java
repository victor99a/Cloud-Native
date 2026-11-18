package com.pedidos360.kafkaadminservice;

import com.pedidos360.kafkaadminservice.controller.KafkaAdminController;
import com.pedidos360.kafkaadminservice.dto.ConsumerGroupOffsets;
import com.pedidos360.kafkaadminservice.service.KafkaAdminService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la API REST del administrador de Kafka (capa web aislada).
 */
@WebMvcTest(KafkaAdminController.class)
class KafkaAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaAdminService adminService;

    @Test
    void creaTopicoValidoDevuelve201() throws Exception {
        when(adminService.createTopic(any())).thenReturn("demo.topico");

        mockMvc.perform(post("/api/v1/kafka/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"demo.topico\",\"partitions\":3,\"replicas\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topic").value("demo.topico"));
    }

    @Test
    void rechazaNombreDeTopicoInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/kafka/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"nombre con espacios\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adminService);
    }

    @Test
    void rechazaResetConDestinoInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/kafka/consumer-groups/grupo/offsets/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"t\",\"to\":\"ayer\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adminService);
    }

    @Test
    void devuelveLagDelConsumerGroup() throws Exception {
        when(adminService.consumerGroupOffsets("grupo")).thenReturn(new ConsumerGroupOffsets("grupo", 4L,
                List.of(new ConsumerGroupOffsets.PartitionOffset("t", 0, 6L, 10L, 4L))));

        mockMvc.perform(get("/api/v1/kafka/consumer-groups/grupo/offsets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLag").value(4))
                .andExpect(jsonPath("$.partitions[0].endOffset").value(10));
    }
}
