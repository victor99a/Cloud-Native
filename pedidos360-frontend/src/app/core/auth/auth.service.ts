import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

/**
 * Fachada de autenticación sobre Keycloak (IDaaS local).
 * Expone la misma API que consumen los componentes y guards, de modo que el
 * cambio de MSAL/Azure AD a Keycloak es transparente para el resto de la app.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private readonly keycloak: KeycloakService) {}

  isAuthenticated(): boolean {
    return this.keycloak.isLoggedIn();
  }

  /** Nombre visible del usuario (given_name + family_name o username). */
  get userName(): string {
    const token = this.keycloak.getKeycloakInstance().tokenParsed;
    if (!token) {
      return '';
    }
    const name = [token['given_name'], token['family_name']]
      .filter(Boolean)
      .join(' ')
      .trim();
    return name || token['preferred_username'] || this.keycloak.getUsername() || '';
  }

  /** Roles de realm asignados al usuario (claim realm_access.roles de Keycloak). */
  get userRoles(): string[] {
    const token = this.keycloak.getKeycloakInstance().tokenParsed;
    const realmAccess = token?.['realm_access'] as { roles?: string[] } | undefined;
    const roles = realmAccess?.roles ?? [];
    const fallback = token?.['roles'];
    if (Array.isArray(fallback)) {
      return [...new Set([...roles, ...fallback.map(String)])];
    }
    return roles;
  }

  login(): void {
    // redirectUri por defecto = window.location.origin; al volver, el guard enruta a /dashboard.
    void this.keycloak.login();
  }

  logout(): void {
    void this.keycloak.logout(window.location.origin);
  }
}
