import { KeycloakConfig, KeycloakInitOptions } from 'keycloak-js';
import { environment } from '../../environments/environment';

/**
 * Configuración del cliente OIDC contra Keycloak (IDaaS local).
 * Authorization Code + PKCE (cliente público SPA).
 */
export const keycloakConfig: KeycloakConfig = {
  url: environment.keycloak.url,
  realm: environment.keycloak.realm,
  clientId: environment.keycloak.clientId,
};

/**
 * Opciones de inicialización de keycloak-js.
 * - check-sso: detecta sesión existente sin redirigir de forma forzada.
 * - silentCheckSsoRedirectUri + checkLoginIframe=false: verificación por redirect
 *   (evita depender de iframes y cookies de terceros).
 */
export const keycloakInitOptions: KeycloakInitOptions = {
  onLoad: 'check-sso',
  silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
  checkLoginIframe: false,
  pkceMethod: 'S256',
};
