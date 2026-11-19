import { environment } from '../../environments/environment';

/**
 * Configuración en tiempo de ejecución.
 *
 * <p>En la nube, el contenedor nginx genera {@code /config.json} a partir de variables
 * de entorno (issuer y client id de Cognito, URL de logout, base de la API). Así la
 * MISMA imagen sirve para cualquier cuenta/tenant de AWS sin recompilar. Si el
 * archivo no existe (entorno local), se usan los valores del environment.
 */
interface RuntimeConfig {
  apiGatewayUrl?: string;
  auth?: Partial<typeof environment.auth>;
}

export async function loadRuntimeConfig(): Promise<void> {
  try {
    const response = await fetch('/config.json', { cache: 'no-store' });
    if (!response.ok) {
      return;
    }
    const config = (await response.json()) as RuntimeConfig;
    if (typeof config.apiGatewayUrl === 'string') {
      environment.apiGatewayUrl = config.apiGatewayUrl;
    }
    if (config.auth) {
      Object.assign(environment.auth, config.auth);
    }
  } catch {
    // Sin config.json (desarrollo local): se mantienen los valores compilados.
  }
}
