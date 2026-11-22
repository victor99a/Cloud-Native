import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PedidosStoreService } from '../services/pedidos-store.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { estadoLabel } from '../models/pedido.model';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { LoaderComponent } from '../../../shared/components/loader/loader.component';
import { OrderStatusBadgeComponent } from '../components/order-status-badge.component';

@Component({
  selector: 'app-pedido-detail',
  standalone: true,
  imports: [
    DecimalPipe,
    RouterLink,
    NavbarComponent,
    ButtonComponent,
    LoaderComponent,
    OrderStatusBadgeComponent,
    ModalComponent,
  ],
  template: `
    <app-navbar />

    <section class="page">
      <a routerLink="/pedidos" class="back-link">&larr; Volver a pedidos</a>

      <app-loader [isLoading]="store.isLoading()" />

      @if (store.error(); as error) {
        <div class="error-message" role="alert">{{ error }}</div>
      }

      @if (store.accionError(); as accionError) {
        <div class="error-message action-error" role="alert">
          <span>{{ accionError }}</span>
          <button type="button" aria-label="Cerrar aviso" (click)="store.limpiarAccionError()">✕</button>
        </div>
      }

      @if (!store.isLoading() && !store.error()) {
        @if (pedido(); as pedido) {
          <div class="card detail-card">
            <div class="card-header">
              <div>
                <h1 class="title">Pedido {{ pedido.numero }}</h1>
                <p class="cliente">Cliente: {{ pedido.cliente }}</p>
              </div>
              <app-order-status-badge [estado]="pedido.estado" />
            </div>

            <ul class="items-list">
              @for (item of pedido.items; track item.productoId) {
                <li>
                  <span>{{ item.nombreProducto }} × {{ item.cantidad }}</span>
                  <span>&#36;{{ item.subtotal | number: '1.0-0' }}</span>
                </li>
              }
            </ul>

            <p class="total">
              Total: <strong>&#36;{{ pedido.total | number: '1.0-0' }}</strong>
            </p>

            <div class="actions">
              @if (!siguienteEstado() && !puedeCancelar()) {
                <p class="info">Este pedido ya llegó a su estado final.</p>
              } @else if (!puedeGestionar()) {
                <p class="info">Solo un administrador u operador puede cambiar el estado de este pedido.</p>
              } @else {
                @if (siguienteEstado(); as siguiente) {
                  <app-button
                    [label]="'Avanzar a ' + etiquetaEstado(siguiente)"
                    [loading]="store.guardando()"
                    (clicked)="avanzar()"
                  />
                }
                @if (puedeCancelar()) {
                  <app-button
                    label="Cancelar pedido"
                    variant="danger"
                    [disabled]="store.guardando()"
                    (clicked)="confirmarCancelacion = true"
                  />
                }
              }
            </div>
          </div>
        } @else {
          <div class="empty-state">
            <div class="empty-icon material-symbols-outlined" aria-hidden="true">search</div>
            <p>No se encontró el pedido.</p>
          </div>
        }
      }
      <app-modal title="¿Cancelar este pedido?" [isOpen]="confirmarCancelacion" (close)="confirmarCancelacion = false">
        <p class="confirm-text">
          El pedido {{ pedido()?.numero }} pasará a <strong>Cancelado</strong>. Esta acción no se puede deshacer.
        </p>
        <div class="confirm-actions">
          <app-button label="Volver" variant="secondary" (clicked)="confirmarCancelacion = false" />
          <app-button label="Sí, cancelar pedido" variant="danger" (clicked)="cancelar()" />
        </div>
      </app-modal>
    </section>
  `,
  styles: `
    .back-link {
      display: inline-flex;
      align-items: center;
      min-height: 40px;
      margin-bottom: 16px;
      color: var(--color-text-muted);
      text-decoration: none;
      font-size: 14px;
      font-weight: 500;
      padding: 6px 10px;
      border-radius: var(--radius-sm);
      transition: background var(--transition), color var(--transition);
    }

    .back-link:hover {
      background: var(--color-surface);
      color: var(--color-text);
    }

    .detail-card {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      flex-wrap: wrap;
      gap: 12px;
    }

    .title {
      margin: 0 0 4px;
      font-size: clamp(1.25rem, 3vw, 1.5rem);
    }

    .cliente {
      margin: 0;
    }

    .items-list {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .items-list li {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      background: var(--color-bg);
      padding: 10px 14px;
      border-radius: var(--radius-sm);
      font-size: 14px;
    }

    .total {
      font-size: 20px;
      margin: 0;
      color: var(--color-text);
    }

    .total strong {
      color: var(--color-primary);
    }

    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      padding-top: 4px;
      border-top: 1px solid var(--color-border);
    }

    .info {
      color: var(--color-text-muted);
      margin: 0;
      align-self: center;
    }

    .confirm-text {
      color: var(--color-text);
      margin: 0 0 20px;
    }

    .confirm-actions {
      display: flex;
      justify-content: flex-end;
      flex-wrap: wrap;
      gap: 8px;
    }
  `,
})
export class PedidoDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly store = inject(PedidosStoreService);
  private readonly authService = inject(AuthService);

  /** Muestra el diálogo de confirmación antes de cancelar. */
  confirmarCancelacion = false;

  private readonly id = Number(this.route.snapshot.paramMap.get('id'));

  ngOnInit(): void {
    this.store.cargar();
  }

  pedido() {
    return this.store.findById(this.id);
  }

  siguienteEstado() {
    const pedido = this.pedido();
    return pedido ? this.store.siguienteEstado(pedido.estado) : null;
  }

  /** El BFF solo permite cambiar estados a ADMIN y OPERADOR (los demás reciben 403). */
  puedeGestionar(): boolean {
    const roles = this.authService.userRoles.map((r) => r.toUpperCase());
    return roles.includes('ADMIN') || roles.includes('OPERADOR');
  }

  puedeCancelar(): boolean {
    const estado = this.pedido()?.estado;
    return !!estado && estado !== 'ENTREGADO' && estado !== 'CANCELADO';
  }

  etiquetaEstado(estado: string): string {
    return estadoLabel(estado);
  }

  avanzar(): void {
    this.store.avanzarEstado(this.id);
  }

  cancelar(): void {
    this.confirmarCancelacion = false;
    this.store.cancelar(this.id);
  }
}
