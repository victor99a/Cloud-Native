import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
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
      <div class="header-row">
        <div>
          <h1>Pedidos</h1>
          <p>Datos reales, consumidos desde el API Gateway.</p>
        </div>
        <app-button label="Crear pedido" (clicked)="abrirModal()" />
      </div>

      <app-loader [isLoading]="store.isLoading()" />

      @if (store.error(); as error) {
        <p class="error">{{ error }}</p>
      }

      @if (!store.isLoading()) {
        <div class="filters">
          <input
            type="text"
            placeholder="Buscar por cliente..."
            [(ngModel)]="filtroCliente"
            name="filtroCliente"
          />
          <select [(ngModel)]="filtroEstado" name="filtroEstado">
            <option value="">Todos los estados</option>
            @for (estado of estados; track estado) {
              <option [value]="estado">{{ estado }}</option>
            }
          </select>
        </div>

        @if (pedidosFiltrados().length === 0) {
          <p>No hay pedidos que coincidan con el filtro.</p>
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
                  <tr [routerLink]="['/pedidos', pedido.id]" class="clickable">
                    <td>{{ pedido.numero }}</td>
                    <td>{{ pedido.cliente }}</td>
                    <td><app-order-status-badge [estado]="pedido.estado" /></td>
                    <td>{{ pedido.total | number: '1.0-0' }}</td>
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
            <input type="text" name="cliente" [(ngModel)]="nuevoCliente" required />
          </label>

          <div class="item-picker">
            <label>
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
            <app-button label="Agregar" variant="secondary" (clicked)="agregarItem()" />
          </div>

          @if (items.length > 0) {
            <ul class="items-list">
              @for (item of items; track item.productoId; let i = $index) {
                <li>
                  {{ nombreProducto(item.productoId) }} × {{ item.cantidad }}
                  <button type="button" class="remove-btn" (click)="quitarItem(i)">✕</button>
                </li>
              }
            </ul>
          } @else {
            <p class="hint">Agrega al menos un producto al pedido.</p>
          }

          <div class="form-actions">
            <app-button label="Cancelar" variant="secondary" (clicked)="cerrarModal()" />
            <button type="submit" class="submit-btn" [disabled]="!nuevoCliente || items.length === 0">
              Guardar
            </button>
          </div>
        </form>
      </app-modal>
    </section>
  `,
  styles: `
    .page {
      max-width: 960px;
      margin: 0 auto;
      padding: 0 24px 40px;
    }
    .header-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;
      gap: 16px;
    }
    .error {
      color: #dc2626;
    }
    .filters {
      display: flex;
      gap: 10px;
      margin-bottom: 16px;
    }
    .filters input,
    .filters select {
      padding: 8px 10px;
      border: 1px solid #e2e4ec;
      border-radius: 6px;
      font-size: 14px;
    }
    .filters input {
      flex: 1;
    }
    .table-wrap {
      overflow-x: auto;
    }
    tr.clickable {
      cursor: pointer;
    }
    form {
      display: flex;
      flex-direction: column;
      gap: 14px;
      min-width: 300px;
    }
    label {
      display: flex;
      flex-direction: column;
      gap: 4px;
      font-size: 13px;
      color: #6b7086;
    }
    input,
    select {
      padding: 8px 10px;
      border: 1px solid #e2e4ec;
      border-radius: 6px;
      font-size: 14px;
    }
    .item-picker {
      display: flex;
      align-items: flex-end;
      gap: 8px;
    }
    .item-picker select {
      min-width: 160px;
    }
    .cantidad input {
      width: 70px;
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
      background: #f4f5f9;
      padding: 6px 10px;
      border-radius: 6px;
      font-size: 14px;
    }
    .remove-btn {
      border: none;
      background: transparent;
      cursor: pointer;
      color: #dc2626;
    }
    .hint {
      color: #6b7086;
      font-size: 13px;
      margin: 0;
    }
    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      margin-top: 4px;
    }
    .submit-btn {
      border: none;
      border-radius: 8px;
      padding: 10px 18px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      background: #4f46e5;
      color: white;
    }
    .submit-btn:hover:not(:disabled) {
      background: #4338ca;
    }
    .submit-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  `,
})
export class PedidosListComponent implements OnInit {
  readonly store = inject(PedidosStoreService);
  readonly productosStore = inject(ProductosStoreService);

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
    if (!this.nuevoCliente || this.items.length === 0) {
      return;
    }
    this.store.crear({ cliente: this.nuevoCliente, items: this.items });
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
