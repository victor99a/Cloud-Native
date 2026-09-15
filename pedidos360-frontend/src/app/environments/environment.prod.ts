// Configuración de producción: los valores se reemplazan en el despliegue real
// (EC2 + API Gateway). El audience (API_APP_ID) debe coincidir con el del BFF.
export const environment = {
  production: true,
  apiGatewayUrl: 'https://<API_GATEWAY_URL>/api',
  azureAd: {
    tenantId: '<TENANT_ID>',
    clientId: '<CLIENT_ID>',
    authority: 'https://login.microsoftonline.com/<TENANT_ID>',
    redirectUri: 'https://<FRONTEND_DOMAIN>',
    postLogoutRedirectUri: 'https://<FRONTEND_DOMAIN>',
    scopes: ['api://<API_APP_ID>/access_as_user'],
  },
};
