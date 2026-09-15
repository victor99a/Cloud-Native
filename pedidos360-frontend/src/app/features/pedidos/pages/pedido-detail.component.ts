import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PedidosStoreService } from '../services/pedidos-store.service';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { OrderStatusBadgeComponent } from '../components/order-status-badge.component';

@Component({
  selector: 'app-pedido-detail',
  standalone: true,
  imports: [DecimalPipe, RouterLink, NavbarComponent, ButtonComponent, OrderStatusBadgeComponent],
  template: `
    <app-navbar />

    <section class="page">
      <a routerLink="/pedidos" class="back-link">&larr; Volver a pedidos</a>

      @if (pedido(); as pedido) {
        <div class="card">
          <div class="card-header">
            <div>
              <h1>Pedido #{{ pedido.id }}</h1>
              <p>Cliente: {{ pedido.cliente }}</p>
            </div>
            <app-order-status-badge [estado]="pedido.estado" />
          </div>

          <p class="total">Total: {{ pedido.total | number: '1.0-0' }}</p>

          @if (siguienteEstado(); as siguiente) {
            <app-button [label]="'Avanzar a ' + siguiente" (clicked)="avanzar()" />
          } @else {
            <p class="info">Este pedido ya llegó a su estado final.</p>
          }

          @if (pedido.estado !== 'ENTREGADO' && pedido.estado !== 'CANCELADO') {
            <app-button label="Cancelar pedido" variant="secondary" (clicked)="cancelar()" />
          }
        </div>
      } @else {
        <p>No se encontró el pedido.</p>
      }
    </section>
  `,
  styles: `
    .page {
      max-width: 640px;
      margin: 0 auto;
      padding: 0 24px 40px;
    }
    .back-link {
      display: inline-block;
      margin-bottom: 16px;
      color: #6b7086;
      text-decoration: none;
      font-size: 14px;
    }
    .back-link:hover {
      color: #1f2333;
    }
    .card {
      background: white;
      border-radius: 10px;
      padding: 24px;
      box-shadow: 0 1px 3px rgba(15, 15, 35, 0.08);
      display: flex;
      flex-direction: column;
      gap: 14px;
    }
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }
    .total {
      font-size: 20px;
      font-weight: 700;
      margin: 0;
    }
    .info {
      color: #6b7086;
      margin: 0;
    }
  `,
})
export class PedidoDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(PedidosStoreService);

  private readonly id = Number(this.route.snapshot.paramMap.get('id'));

  pedido() {
    return this.store.findById(this.id);
  }

  siguienteEstado() {
    const pedido = this.pedido();
    return pedido ? this.store.siguienteEstado(pedido.estado) : null;
  }

  avanzar(): void {
    this.store.avanzarEstado(this.id);
  }

  cancelar(): void {
    this.store.cancelar(this.id);
  }
}
