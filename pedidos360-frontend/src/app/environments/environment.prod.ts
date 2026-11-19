// Configuración de producción local (contenedor nginx en Docker Compose).
// Mismos endpoints locales: Keycloak en :8090 y Gateway en :8080.
export const environment = {
  production: true,
  apiGatewayUrl: 'http://localhost:8080',
  auth: {
    issuer: 'http://localhost:8090/realms/pedidos360',
    clientId: 'pedidos360-spa',
    scope: 'openid profile email',
    requireHttps: false,
    logoutUrl: null as string | null,
  },
};
