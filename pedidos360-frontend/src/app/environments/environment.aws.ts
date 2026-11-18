// Configuración de despliegue en AWS.
// - IDaaS: Amazon Cognito (User Pool "pedidos360-tenant").
// - API Manager: Amazon API Gateway (HTTP API). El frontend se sirve desde el mismo
//   origen HTTPS que la API, por eso apiGatewayUrl es relativo ('' -> /api/...).
const cognitoDomain = 'https://pedidos360-381492248228.auth.us-east-1.amazoncognito.com';
const clientId = '3co912n6as17utm5q0a07upsrl';

export const environment = {
  production: true,
  apiGatewayUrl: '',
  auth: {
    issuer: 'https://cognito-idp.us-east-1.amazonaws.com/us-east-1_NUy3OxPyD',
    clientId,
    scope: 'openid profile email',
    requireHttps: true,
    // Cognito no publica end_session_endpoint: se usa su endpoint /logout.
    logoutUrl: `${cognitoDomain}/logout?client_id=${clientId}&logout_uri=` as string | null,
  },
};
