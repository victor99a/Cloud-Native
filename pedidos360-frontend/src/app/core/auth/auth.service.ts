import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { buildAuthConfig } from './auth-config';

type Claims = Record<string, unknown>;

/**
 * Fachada de autenticación OIDC (Amazon Cognito en AWS / Keycloak en local).
 * Expone la misma API que consumen componentes y guards, de modo que el IDaaS
 * concreto es transparente para el resto de la app.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private readonly oauth: OAuthService) {}

  /**
   * Se ejecuta antes de arrancar la app: carga el discovery document del IDaaS y,
   * si la URL trae un ?code= (vuelta del login), lo canjea por tokens (PKCE).
   */
  async init(): Promise<void> {
    this.oauth.configure(buildAuthConfig());
    try {
      await this.oauth.loadDiscoveryDocumentAndTryLogin();
      if (this.oauth.hasValidAccessToken()) {
        this.oauth.setupAutomaticSilentRefresh();
      }
    } catch (error) {
      // Si el IDaaS no responde, la app arranca igual y muestra la pantalla de login.
      console.error('[auth] No se pudo inicializar OIDC', error);
    }
  }

  isAuthenticated(): boolean {
    return this.oauth.hasValidAccessToken();
  }

  /** Access token (JWT) que se envía como Bearer al API Manager. */
  get accessToken(): string {
    return this.oauth.getAccessToken();
  }

  /** Nombre visible del usuario (claims del ID token). */
  get userName(): string {
    const claims = (this.oauth.getIdentityClaims() ?? {}) as Claims;
    const fullName = [claims['given_name'], claims['family_name']].filter(Boolean).join(' ').trim();
    return (
      fullName ||
      String(claims['name'] ?? '') ||
      String(claims['preferred_username'] ?? '') ||
      String(claims['cognito:username'] ?? '') ||
      String(claims['email'] ?? '')
    );
  }

  /**
   * Roles del usuario. Keycloak: realm_access.roles / roles.
   * Cognito: grupos del User Pool en cognito:groups.
   */
  get userRoles(): string[] {
    const sources = [this.decode(this.oauth.getAccessToken()), (this.oauth.getIdentityClaims() ?? {}) as Claims];
    const roles = new Set<string>();
    for (const claims of sources) {
      const realmAccess = claims['realm_access'] as { roles?: unknown[] } | undefined;
      [realmAccess?.roles, claims['roles'], claims['cognito:groups']]
        .filter(Array.isArray)
        .forEach((list) => (list as unknown[]).forEach((r) => roles.add(String(r))));
    }
    return [...roles];
  }

  login(): void {
    this.oauth.initCodeFlow();
  }

  logout(): void {
    this.oauth.logOut();
  }

  private decode(token: string | null): Claims {
    if (!token) {
      return {};
    }
    try {
      const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(decodeURIComponent(escape(atob(payload)))) as Claims;
    } catch {
      return {};
    }
  }
}
