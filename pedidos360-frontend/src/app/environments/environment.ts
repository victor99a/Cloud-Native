// Configuración de Azure AD (Entra ID) y API Gateway.
// Reemplaza los placeholders con los valores del App Registration "Pedidos360";
// TENANT_ID, CLIENT_ID y API_APP_ID deben coincidir con los que valida el BFF.
export const environment = {
  production: false,
  apiGatewayUrl: 'https://jkvzl52faj.execute-api.us-east-1.amazonaws.com/api',
  azureAd: {
    tenantId: '840f00c4-5744-42d5-8827-ed48c11ff4d4',
    clientId: 'b4a1d505-4e63-4d74-ad9a-5f7baf82b7dd',
    authority: 'https://login.microsoftonline.com/840f00c4-5744-42d5-8827-ed48c11ff4d4',
    redirectUri: 'http://localhost:4200',
    postLogoutRedirectUri: 'http://localhost:4200',
    scopes: ['api://b4a1d505-4e63-4d74-ad9a-5f7baf82b7dd/access_as_user'],
  },
};
