// Configuración de producción (contenedor nginx en Docker Compose).
// Mismos endpoints locales: Keycloak en :8090 y Gateway en :8080.
export const environment = {
  production: true,
  apiGatewayUrl: 'http://localhost:8080',
  keycloak: {
    url: 'http://localhost:8090',
    realm: 'pedidos360',
    clientId: 'pedidos360-spa',
  },
};
