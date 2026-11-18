// Configuración local: Keycloak (IDaaS local) + API Gateway (Spring Cloud Gateway).
// El frontend consume exclusivamente el Gateway (punto único de entrada).
export const environment = {
  production: false,
  apiGatewayUrl: 'http://localhost:8080',
  auth: {
    // Emisor OIDC (discovery en <issuer>/.well-known/openid-configuration).
    issuer: 'http://localhost:8090/realms/pedidos360',
    clientId: 'pedidos360-spa',
    scope: 'openid profile email',
    // Keycloak local corre por HTTP.
    requireHttps: false,
    // Keycloak publica end_session_endpoint en el discovery: no hace falta URL manual.
    logoutUrl: null as string | null,
  },
};
