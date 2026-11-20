package com.pedidos360.gateway.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Acepta solo <b>access tokens</b>. Un ID token (OIDC) identifica al usuario ante el
 * frontend, pero no autoriza llamadas a la API; como lo firma el mismo IDaaS y lleva
 * el cliente en {@code aud}, pasaría las validaciones de firma, issuer y audience.
 * <ul>
 *   <li>Amazon Cognito: claim {@code token_use} = {@code access} (el ID token trae {@code id}).</li>
 *   <li>Keycloak: claim {@code typ} = {@code Bearer} (el ID token trae {@code ID}).</li>
 * </ul>
 */
public class AccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        boolean idTokenCognito = "id".equalsIgnoreCase(jwt.getClaimAsString("token_use"));
        boolean idTokenKeycloak = "ID".equalsIgnoreCase(jwt.getClaimAsString("typ"));
        if (idTokenCognito || idTokenKeycloak) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                    "Se requiere un access token: los ID tokens no autorizan llamadas a la API", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
