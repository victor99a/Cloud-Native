package com.pedidos360.bff.security;

import java.util.Collection;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AzureAdRolesConverter rolesConverter;

    public JwtAuthenticationConverter(AzureAdRolesConverter rolesConverter) {
        this.rolesConverter = rolesConverter;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = rolesConverter.convert(jwt);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
