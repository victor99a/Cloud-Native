import { Component, Input } from '@angular/core';
import { EstadoPedido } from '../models/pedido.model';

@Component({
  selector: 'app-order-status-badge',
  standalone: true,
  template: ` <span [class]="'badge estado-' + estado.toLowerCase()">{{ estado }}</span> `,
  styles: `
    .badge {
      display: inline-block;
      padding: 4px 12px;
      border-radius: var(--radius-full);
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.01em;
      white-space: nowrap;
      background: var(--color-primary-soft);
      color: var(--color-primary);
    }

    .badge.estado-pendiente {
      background: var(--color-warning-soft);
      color: var(--color-warning);
    }

    .badge.estado-confirmado {
      background: var(--color-info-soft);
      color: var(--color-info);
    }

    .badge.estado-en_preparacion {
      background: var(--color-primary-soft);
      color: var(--color-primary);
    }

    .badge.estado-enviado {
      background: var(--color-teal-soft);
      color: var(--color-teal);
    }

    .badge.estado-entregado {
      background: var(--color-success-soft);
      color: var(--color-success);
    }

    .badge.estado-cancelado {
      background: var(--color-danger-soft);
      color: var(--color-danger);
    }
  `,
})
export class OrderStatusBadgeComponent {
  @Input({ required: true }) estado!: EstadoPedido;
}
