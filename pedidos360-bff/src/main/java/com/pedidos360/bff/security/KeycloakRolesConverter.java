package com.pedidos360.bff.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Convierte los roles del JWT emitido por Keycloak en authorities de Spring Security.
 *
 * <p>Keycloak expone los roles de realm en el claim {@code realm_access.roles}
 * (estructura estándar). Como respaldo, también se consideran el claim plano
 * {@code roles} (mapper del client scope "roles") y los roles por cliente en
 * {@code resource_access.*.roles}, para ser robusto ante distintas configuraciones.
 */
@Component
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();

        collectRoles(jwt.getClaimAsMap("realm_access"), roles);
        roles.addAll(nullSafe(jwt.getClaimAsStringList("roles")));

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().stream()
                    .filter(Map.class::isInstance)
                    .map(v -> (Map<String, Object>) v)
                    .forEach(entry -> collectRoles(entry, roles));
        }

        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    private void collectRoles(Map<String, Object> container, Set<String> target) {
        if (container == null) {
            return;
        }
        Object roles = container.get("roles");
        if (roles instanceof List<?> list) {
            list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .forEach(target::add);
        }
    }

    private List<String> nullSafe(List<String> value) {
        return value == null ? List.of() : value;
    }
}
