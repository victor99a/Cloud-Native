import { AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../environments/environment';

/**
 * Cliente OIDC genérico (OAuth 2.0 Authorization Code + PKCE, cliente público SPA).
 * Funciona con cualquier IDaaS compatible con OpenID Connect: Amazon Cognito en
 * AWS y Keycloak en local; solo cambia el environment.
 */
export function buildAuthConfig(): AuthConfig {
  const origin = window.location.origin;
  const logoutUrl = environment.auth.logoutUrl
    ? environment.auth.logoutUrl + encodeURIComponent(origin + '/')
    : undefined;

  return {
    issuer: environment.auth.issuer,
    clientId: environment.auth.clientId,
    redirectUri: origin + '/',
    postLogoutRedirectUri: origin + '/',
    responseType: 'code',
    scope: environment.auth.scope,
    requireHttps: environment.auth.requireHttps,
    // Cognito publica sus endpoints (authorize/token) en un dominio distinto al issuer.
    strictDiscoveryDocumentValidation: false,
    logoutUrl,
    // Renovación con refresh_token (no iframes ni cookies de terceros).
    useSilentRefresh: false,
    timeoutFactor: 0.75,
    showDebugInformation: !environment.production,
  };
}
