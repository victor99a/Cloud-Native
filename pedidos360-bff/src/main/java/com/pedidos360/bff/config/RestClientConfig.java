package com.pedidos360.bff.config;

import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // El SimpleClientHttpRequestFactory por defecto usa HttpURLConnection, que no
    // soporta el método PATCH (java.net.ProtocolException: Invalid HTTP method).
    // JdkClientHttpRequestFactory usa java.net.http.HttpClient (Java 11+), que sí lo soporta.
    private JdkClientHttpRequestFactory jdkRequestFactory() {
        return new JdkClientHttpRequestFactory(HttpClient.newHttpClient());
    }

    @Bean
    public RestClient pedidosRestClient(RestClient.Builder builder,
                                        @Value("${downstream.pedidos.base-url:http://localhost:8081}") String baseUrl) {
        return builder.baseUrl(baseUrl).requestFactory(jdkRequestFactory()).build();
    }

    @Bean
    public RestClient productosRestClient(RestClient.Builder builder,
                                          @Value("${downstream.productos.base-url:http://localhost:8082}") String baseUrl) {
        return builder.baseUrl(baseUrl).requestFactory(jdkRequestFactory()).build();
    }
}
