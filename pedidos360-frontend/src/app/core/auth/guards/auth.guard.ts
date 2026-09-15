import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth.service';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authService = inject(AuthService);

  if (authService.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/login']);
};

export function rolesGuard(allowedRoles: string[]): CanActivateFn {
  return () => {
    const router = inject(Router);
    const authService = inject(AuthService);

    const userRoles = authService.userRoles.map((role) => role.toUpperCase());
    if (userRoles.some((role) => allowedRoles.includes(role))) {
      return true;
    }
    return router.createUrlTree(['/dashboard']);
  };
}