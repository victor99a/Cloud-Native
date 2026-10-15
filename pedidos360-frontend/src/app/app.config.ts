import {
  APP_INITIALIZER,
  ApplicationConfig,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptors,
  withInterceptorsFromDi,
} from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { KeycloakBearerInterceptor, KeycloakService } from 'keycloak-angular';
import { routes } from './app.routes';
import { keycloakConfig, keycloakInitOptions } from './core/auth/keycloak-config';
import { loadingInterceptor } from './core/interceptors/loading.interceptor';
import { httpErrorInterceptor } from './core/interceptors/http-error.interceptor';

/**
 * Inicializa Keycloak antes de arrancar la aplicación (check-sso).
 * keycloak-angular v16 expone la API clásica (KeycloakService.init), no
 * provideKeycloak (que es de v19+).
 */
function initializeKeycloak(keycloak: KeycloakService): () => Promise<boolean> {
  return () =>
    keycloak.init({
      config: keycloakConfig,
      initOptions: keycloakInitOptions,
      enableBearerInterceptor: true,
      loadUserProfileAtStartUp: false,
    });
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideAnimationsAsync(),
    provideHttpClient(
      withInterceptors([loadingInterceptor, httpErrorInterceptor]),
      withInterceptorsFromDi(),
    ),
    // Servicio de autenticación de keycloak-angular (no es providedIn: 'root').
    KeycloakService,
    // Interceptor que adjunta el Bearer token a las peticiones HTTP salientes.
    {
      provide: HTTP_INTERCEPTORS,
      useClass: KeycloakBearerInterceptor,
      multi: true,
    },
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      deps: [KeycloakService],
      multi: true,
    },
  ],
};
