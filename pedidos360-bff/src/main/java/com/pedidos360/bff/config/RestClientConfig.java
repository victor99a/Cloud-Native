package com.pedidos360.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient pedidosRestClient(RestClient.Builder builder,
                                        @Value("${downstream.pedidos.base-url:http://localhost:8081}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public RestClient productosRestClient(RestClient.Builder builder,
                                          @Value("${downstream.productos.base-url:http://localhost:8082}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
