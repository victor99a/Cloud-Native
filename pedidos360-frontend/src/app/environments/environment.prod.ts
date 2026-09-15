// Configuración de producción (EC2 + API Gateway + Azure AD real del equipo).
export const environment = {
  production: true,
  apiGatewayUrl: 'https://jkvzl52faj.execute-api.us-east-1.amazonaws.com/api',
  azureAd: {
    tenantId: '840f00c4-5744-42d5-8827-ed48c11ff4d4',
    clientId: 'b4a1d505-4e63-4d74-ad9a-5f7baf82b7dd',
    authority: 'https://login.microsoftonline.com/840f00c4-5744-42d5-8827-ed48c11ff4d4',
    redirectUri: 'https://44.194.250.1',
    postLogoutRedirectUri: 'https://44.194.250.1',
    scopes: ['api://b4a1d505-4e63-4d74-ad9a-5f7baf82b7dd/access_as_user'],
  },
};
