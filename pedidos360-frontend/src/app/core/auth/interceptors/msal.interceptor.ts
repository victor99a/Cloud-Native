import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { from, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth.service';

export const msalInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.headers.has('Authorization') || !req.url.startsWith(environment.apiGatewayUrl)) {
    return next(req);
  }

  const authService = inject(AuthService);
  const msalService = inject(MsalService);

  return from(acquireToken(authService, msalService)).pipe(
    switchMap((token) => {
      if (!token) {
        return next(req);
      }
      return next(req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) }));
    }),
  );
};

async function acquireToken(authService: AuthService, msalService: MsalService): Promise<string> {
  const cached = authService.getAccessToken();
  if (cached) {
    return cached;
  }
  try {
    const token = await authService.acquireTokenSilently();
    if (token) {
      return token;
    }
  } catch {
    return '';
  }
  return loginRedirectWhenRequired(msalService);
}

function loginRedirectWhenRequired(msalService: MsalService): string {
  void msalService.loginRedirect({ scopes: environment.azureAd.scopes });
  return '';
}