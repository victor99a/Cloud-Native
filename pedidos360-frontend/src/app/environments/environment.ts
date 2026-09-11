// Configuración de Azure AD (Entra ID) y API Gateway.
// Reemplaza los placeholders con los valores del App Registration "Pedidos360";
// TENANT_ID, CLIENT_ID y API_APP_ID deben coincidir con los que valida el BFF.
export const environment = {
  production: false,
  apiGatewayUrl: 'http://localhost:8080/api',
  azureAd: {
    tenantId: '<TENANT_ID>',
    clientId: '<CLIENT_ID>',
    authority: 'https://login.microsoftonline.com/<TENANT_ID>',
    redirectUri: 'http://localhost:4200',
    postLogoutRedirectUri: 'http://localhost:4200',
    scopes: ['api://<API_APP_ID>/access_as_user'],
  },
};
