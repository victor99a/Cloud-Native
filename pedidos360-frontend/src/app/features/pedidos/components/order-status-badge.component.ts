import { Component, Input } from '@angular/core';
import { EstadoPedido } from '../models/pedido.model';

@Component({
  selector: 'app-order-status-badge',
  standalone: true,
  template: ` <span [class]="'badge ' + estado.toLowerCase()">{{ estado }}</span> `,
  styles: `
    .badge {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 600;
      background: #eef2ff;
      color: #4f46e5;
    }
    .badge.entregado {
      background: #dcfce7;
      color: #16a34a;
    }
    .badge.cancelado {
      background: #fee2e2;
      color: #dc2626;
    }
  `,
})
export class OrderStatusBadgeComponent {
  @Input({ required: true }) estado!: EstadoPedido;
}
