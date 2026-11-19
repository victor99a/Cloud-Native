package com.pedidos360.gateway.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Garantiza que las peticiones de negocio lleguen a través del API Manager.
 *
 * <p>En la nube, Amazon API Gateway agrega a cada petición un encabezado con un
 * secreto compartido ({@code X-Origin-Verify}). Si el secreto está configurado
 * ({@code ORIGIN_SECRET}), toda petición a {@code /api/**} que no lo traiga se
 * rechaza con 403: llamar directo a la EC2 (puerto 8080) ya no permite saltarse
 * el API Manager ni su authorizer JWT. Sin secreto (entorno local) el filtro no
 * actúa. Se ejecuta antes que Spring Security.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OriginVerifyFilter implements WebFilter {

    public static final String HEADER = "X-Origin-Verify";

    private final byte[] secreto;

    public OriginVerifyFilter(@Value("${pedidos360.origin-secret:}") String secreto) {
        this.secreto = secreto == null ? new byte[0] : secreto.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (secreto.length == 0 || !path.startsWith("/api/")) {
            return chain.filter(exchange);
        }
        String recibido = exchange.getRequest().getHeaders().getFirst(HEADER);
        // Comparación en tiempo constante (no filtra el secreto por tiempos de respuesta).
        if (recibido != null && MessageDigest.isEqual(secreto, recibido.getBytes(StandardCharsets.UTF_8))) {
            return chain.filter(exchange);
        }
        log.warn("[origen] petición directa rechazada (sin pasar por el API Manager) path={} ip={}",
                path, exchange.getRequest().getRemoteAddress());
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }
}
