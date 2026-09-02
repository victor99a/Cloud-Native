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
