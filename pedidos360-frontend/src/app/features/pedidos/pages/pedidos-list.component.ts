import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EstadoPedido, Pedido } from '../models/pedido.model';
import { PedidosStoreService } from '../services/pedidos-store.service';
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
          <p>Datos de ejemplo (todavía no vienen del backend).</p>
        </div>
        <app-button label="Crear pedido" (clicked)="mostrarModal = true" />
      </div>

      <app-loader [isLoading]="isLoading" />

      @if (!isLoading) {
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
                  <th>ID</th>
                  <th>Cliente</th>
                  <th>Estado</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                @for (pedido of pedidosFiltrados(); track pedido.id) {
                  <tr [routerLink]="['/pedidos', pedido.id]" class="clickable">
                    <td>{{ pedido.id }}</td>
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

          <label>
            Total
            <input type="number" name="total" [(ngModel)]="nuevoTotal" required min="0" />
          </label>

          <div class="form-actions">
            <app-button label="Cancelar" variant="secondary" (clicked)="cerrarModal()" />
            <button type="submit" class="submit-btn">Guardar</button>
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
      min-width: 260px;
    }
    label {
      display: flex;
      flex-direction: column;
      gap: 4px;
      font-size: 13px;
      color: #6b7086;
    }
    input {
      padding: 8px 10px;
      border: 1px solid #e2e4ec;
      border-radius: 6px;
      font-size: 14px;
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
    .submit-btn:hover {
      background: #4338ca;
    }
  `,
})
export class PedidosListComponent implements OnInit {
  // inject() en vez de "new": así todos los componentes comparten LA MISMA
  // instancia del servicio (y por lo tanto los mismos datos).
  private readonly store = inject(PedidosStoreService);

  isLoading = true;
  mostrarModal = false;

  nuevoCliente = '';
  nuevoTotal: number | null = null;

  filtroCliente = '';
  filtroEstado: EstadoPedido | '' = '';

  estados: EstadoPedido[] = [
    'CREADO',
    'ACEPTADO',
    'EN_PREPARACION',
    'DESPACHADO',
    'ENTREGADO',
    'CANCELADO',
  ];

  pedidos = this.store.pedidos;

  pedidosFiltrados(): Pedido[] {
    const cliente = this.filtroCliente.toLowerCase().trim();
    const estado = this.filtroEstado;
    return this.pedidos().filter(
      (p) =>
        (!cliente || p.cliente.toLowerCase().includes(cliente)) &&
        (!estado || p.estado === estado),
    );
  }

  ngOnInit(): void {
    // Simula la espera de una llamada al backend (más adelante será un HttpClient real).
    setTimeout(() => {
      this.isLoading = false;
    }, 1000);
  }

  crearPedido(): void {
    if (!this.nuevoCliente || this.nuevoTotal === null) {
      return;
    }
    this.store.crear(this.nuevoCliente, this.nuevoTotal);
    this.cerrarModal();
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.nuevoCliente = '';
    this.nuevoTotal = null;
  }
}
