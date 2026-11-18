import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { environment } from '../../environments/environment';

/**
 * Adjunta el JWT (access token) como Bearer SOLO a las llamadas a nuestra API
 * (prefijo /api del API Manager), nunca a terceros (p. ej. el IDaaS).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const apiPrefix = `${environment.apiGatewayUrl}/api`;
  const token = inject(AuthService).accessToken;

  if (!token || !req.url.startsWith(apiPrefix)) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
