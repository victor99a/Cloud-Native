// Configuración local: Keycloak (IDaaS) + API Gateway (Spring Cloud Gateway).
// El frontend consume exclusivamente el Gateway (punto único de entrada).
export const environment = {
  production: false,
  apiGatewayUrl: 'http://localhost:8080',
  keycloak: {
    url: 'http://localhost:8090',
    realm: 'pedidos360',
    clientId: 'pedidos360-spa',
  },
};
