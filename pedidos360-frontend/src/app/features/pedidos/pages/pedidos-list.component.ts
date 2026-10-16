import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { EstadoPedido, PedidoItemRequest } from '../models/pedido.model';
import { PedidosStoreService } from '../services/pedidos-store.service';
import { ProductosStoreService } from '../../productos/services/productos-store.service';
import { LoaderComponent } from '../../../shared/components/loader/loader.component';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { OrderStatusBadgeComponent } from '../components/order-status-badge.component';

@Component({
  selector: 'app-pedidos-list',
  standalone: true,
  imports: [
    DecimalPipe,
    FormsModule,
    RouterLink,
    LoaderComponent,
    NavbarComponent,
    ButtonComponent,
    ModalComponent,
    OrderStatusBadgeComponent,
  ],
  template: `
    <app-navbar />

    <section class="page">
      <header class="page-header">
        <div>
          <h1 class="page-title">Pedidos</h1>
          <p class="page-subtitle">Consulta y administra los pedidos.</p>
        </div>
        <app-button label="Crear pedido" (clicked)="abrirModal()" />
      </header>

      <app-loader [isLoading]="store.isLoading()" />

      @if (store.error(); as error) {
        <div class="error-message" role="alert">{{ error }}</div>
      }

      @if (!store.isLoading() && !store.error()) {
        <div class="filters">
          <input
            type="text"
            placeholder="Buscar por cliente..."
            aria-label="Buscar por cliente"
            [(ngModel)]="filtroCliente"
            name="filtroCliente"
          />
          <select [(ngModel)]="filtroEstado" name="filtroEstado" aria-label="Filtrar por estado">
            <option value="">Todos los estados</option>
            @for (estado of estados; track estado) {
              <option [value]="estado">{{ estado }}</option>
            }
          </select>
        </div>

        @if (pedidosFiltrados().length === 0) {
          <div class="empty-state">
            <div class="empty-icon material-symbols-outlined" aria-hidden="true">receipt_long</div>
            <p>No hay pedidos que coincidan con el filtro.</p>
          </div>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>N°</th>
                  <th>Cliente</th>
                  <th>Estado</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                @for (pedido of pedidosFiltrados(); track pedido.id) {
                  <tr
                    class="clickable"
                    tabindex="0"
                    role="link"
                    [attr.aria-label]="'Ver pedido ' + pedido.numero"
                    [routerLink]="['/pedidos', pedido.id]"
                    (keydown.enter)="irAlPedido(pedido.id)"
                  >
                    <td>{{ pedido.numero }}</td>
                    <td>{{ pedido.cliente }}</td>
                    <td><app-order-status-badge [estado]="pedido.estado" /></td>
                    <td>&#36;{{ pedido.total | number: '1.0-0' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      }

      <app-modal title="Crear pedido" [isOpen]="mostrarModal" (close)="cerrarModal()">
        <form (ngSubmit)="crearPedido()">
          <label>
            Cliente
            <input type="text" name="cliente" [(ngModel)]="nuevoCliente" required placeholder="Nombre del cliente" />
          </label>

          <div class="item-picker">
            <label class="producto">
              Producto
              <select name="productoId" [(ngModel)]="itemProductoId">
                <option [ngValue]="null">Selecciona...</option>
                @for (producto of productosStore.productos(); track producto.id) {
                  <option [ngValue]="producto.id">{{ producto.nombre }}</option>
                }
              </select>
            </label>
            <label class="cantidad">
              Cantidad
              <input type="number" name="cantidad" [(ngModel)]="itemCantidad" min="1" />
            </label>
            <app-button
              label="Agregar"
              variant="secondary"
              [disabled]="!itemProductoId || itemCantidad < 1"
              (clicked)="agregarItem()"
            />
          </div>

          @if (items.length > 0) {
            <ul class="items-list">
              @for (item of items; track item.productoId; let i = $index) {
                <li>
                  <span>{{ nombreProducto(item.productoId) }} × {{ item.cantidad }}</span>
                  <button
                    type="button"
                    class="remove-btn"
                    [attr.aria-label]="'Quitar ' + nombreProducto(item.productoId)"
                    (click)="quitarItem(i)"
                  >
                    ✕
                  </button>
                </li>
              }
            </ul>
          } @else {
            <p class="hint">Agrega al menos un producto al pedido.</p>
          }

          <div class="form-actions">
            <app-button label="Cancelar" variant="secondary" (clicked)="cerrarModal()" />
            <app-button
              label="Guardar"
              type="submit"
              [disabled]="!nuevoCliente.trim() || items.length === 0"
            />
          </div>
        </form>
      </app-modal>
    </section>
  `,
  styles: `
    .filters {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin-bottom: 16px;
    }

    .filters input {
      flex: 1 1 200px;
    }

    .filters select {
      flex: 0 1 auto;
      min-width: 180px;
    }

    tr.clickable {
      cursor: pointer;
    }

    tr.clickable:focus-visible {
      outline: 2px solid var(--color-primary);
      outline-offset: -2px;
    }

    form {
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .item-picker {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      align-items: flex-end;
    }

    .item-picker .producto {
      flex: 1 1 180px;
    }

    .item-picker .cantidad {
      flex: 0 0 90px;
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
      align-items: center;
      gap: 12px;
      background: var(--color-bg);
      padding: 8px 12px;
      border-radius: var(--radius-sm);
      font-size: 14px;
    }

    .remove-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      border: none;
      background: transparent;
      cursor: pointer;
      color: var(--color-danger);
      border-radius: var(--radius-sm);
      font-size: 14px;
    }

    .remove-btn:hover {
      background: var(--color-danger-soft);
    }

    .hint {
      color: var(--color-text-muted);
      font-size: 13px;
      margin: 0;
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 4px;
    }
  `,
})
export class PedidosListComponent implements OnInit {
  readonly store = inject(PedidosStoreService);
  readonly productosStore = inject(ProductosStoreService);
  private readonly router = inject(Router);

  mostrarModal = false;

  nuevoCliente = '';
  itemProductoId: number | null = null;
  itemCantidad = 1;
  items: PedidoItemRequest[] = [];

  filtroCliente = '';
  filtroEstado: EstadoPedido | '' = '';

  estados: EstadoPedido[] = ['PENDIENTE', 'CONFIRMADO', 'EN_PREPARACION', 'ENVIADO', 'ENTREGADO', 'CANCELADO'];

  ngOnInit(): void {
    this.store.cargar();
    this.productosStore.cargar();
  }

  pedidosFiltrados() {
    const cliente = this.filtroCliente.toLowerCase().trim();
    const estado = this.filtroEstado;
    return this.store
      .pedidos()
      .filter(
        (p) =>
          (!cliente || p.cliente.toLowerCase().includes(cliente)) &&
          (!estado || p.estado === estado),
      );
  }

  nombreProducto(productoId: number): string {
    return this.productosStore.productos().find((p) => p.id === productoId)?.nombre ?? '—';
  }

  irAlPedido(id: number): void {
    this.router.navigate(['/pedidos', id]);
  }

  agregarItem(): void {
    if (!this.itemProductoId || this.itemCantidad < 1) {
      return;
    }
    this.items = [...this.items, { productoId: this.itemProductoId, cantidad: this.itemCantidad }];
    this.itemProductoId = null;
    this.itemCantidad = 1;
  }

  quitarItem(index: number): void {
    this.items = this.items.filter((_, i) => i !== index);
  }

  abrirModal(): void {
    this.mostrarModal = true;
  }

  crearPedido(): void {
    if (!this.nuevoCliente.trim() || this.items.length === 0) {
      return;
    }
    this.store.crear({ cliente: this.nuevoCliente.trim(), items: this.items });
    this.cerrarModal();
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.nuevoCliente = '';
    this.items = [];
    this.itemProductoId = null;
    this.itemCantidad = 1;
  }
}
