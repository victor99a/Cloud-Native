import { Routes } from '@angular/router';
import { authGuard, rolesGuard } from './core/auth/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./core/auth/pages/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'auth/callback',
    loadComponent: () => import('./core/auth/pages/auth-callback.component').then((m) => m.AuthCallbackComponent),
  },
  {
    path: 'pedidos',
    loadChildren: () => import('./features/pedidos/pedidos.routes').then((m) => m.pedidosRoutes),
    canActivate: [authGuard],
  },
  {
    path: 'productos',
    loadChildren: () => import('./features/productos/productos.routes').then((m) => m.productosRoutes),
    canActivate: [authGuard, rolesGuard(['ADMIN', 'OPERADOR'])],
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.routes').then((m) => m.dashboardRoutes),
    canActivate: [authGuard],
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
];