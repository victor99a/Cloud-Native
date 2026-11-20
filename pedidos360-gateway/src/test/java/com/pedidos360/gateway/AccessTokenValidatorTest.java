package com.pedidos360.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.pedidos360.gateway.config.AccessTokenValidator;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/** Solo los access tokens autorizan llamadas a la API; los ID tokens se rechazan. */
class AccessTokenValidatorTest {

    private final AccessTokenValidator validator = new AccessTokenValidator();

    private static Jwt token(String claim, String valor) {
        return Jwt.withTokenValue("t").header("alg", "RS256")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .claim("sub", "usuario").claim(claim, valor).build();
    }

    @Test
    void accessTokenDeCognitoEsAceptado() {
        assertThat(validator.validate(token("token_use", "access")).hasErrors()).isFalse();
    }

    @Test
    void idTokenDeCognitoEsRechazado() {
        assertThat(validator.validate(token("token_use", "id")).hasErrors()).isTrue();
    }

    @Test
    void accessTokenDeKeycloakEsAceptado() {
        assertThat(validator.validate(token("typ", "Bearer")).hasErrors()).isFalse();
    }

    @Test
    void idTokenDeKeycloakEsRechazado() {
        assertThat(validator.validate(token("typ", "ID")).hasErrors()).isTrue();
    }
}
