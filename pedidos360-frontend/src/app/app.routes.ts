import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'pedidos',
    loadChildren: () => import('./features/pedidos/pedidos.routes').then((m) => m.pedidosRoutes),
  },
  {
    path: 'productos',
    loadChildren: () => import('./features/productos/productos.routes').then((m) => m.productosRoutes),
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.routes').then((m) => m.dashboardRoutes),
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
];
