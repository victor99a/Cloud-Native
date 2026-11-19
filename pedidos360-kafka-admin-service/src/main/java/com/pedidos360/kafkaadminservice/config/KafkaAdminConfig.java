package com.pedidos360.kafkaadminservice.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del cliente administrativo de Kafka. Expone un {@link AdminClient}
 * para que el {@code KafkaAdminService} gestione tópicos y consumer groups sin que
 * los controladores manipulen el clúster directamente.
 */
@Configuration
public class KafkaAdminConfig {

    @Bean
    public AdminClient adminClient(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return AdminClient.create(config);
    }
}
