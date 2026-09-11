import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { AuthService } from '../auth/auth.service';

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  if (req.headers.has('Authorization')) {
    return next(req);
  }

  const token = authService.getAccessToken();
  if (token && req.url.startsWith(environment.apiGatewayUrl)) {
    return next(req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) }));
  }

  return next(req);
};