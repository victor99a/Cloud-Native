package com.pedidos360.bff.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente genérico para integrar con microservicios downstream.
 */
@Component
public class DownstreamClient {

    private final RestClient restClient;

    public DownstreamClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public <T> T get(String path, Class<T> responseType) {
        return restClient.get().uri(path).retrieve().body(responseType);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return restClient.post().uri(path).body(body).retrieve().body(responseType);
    }
}
