import { Routes } from '@angular/router';
import { PedidosListComponent } from './pages/pedidos-list.component';
import { PedidoDetailComponent } from './pages/pedido-detail.component';

export const pedidosRoutes: Routes = [
  { path: '', component: PedidosListComponent },
  { path: ':id', component: PedidoDetailComponent },
];
