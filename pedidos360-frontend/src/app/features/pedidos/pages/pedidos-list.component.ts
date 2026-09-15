import { Component, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Pedido } from '../models/pedido.model';
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
        @if (pedidos.length === 0) {
          <p>No hay pedidos todavía.</p>
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
                @for (pedido of pedidos; track pedido.id) {
                  <tr>
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
            <app-button
              label="Cancelar"
              variant="secondary"
              type="button"
              (clicked)="cerrarModal()"
            />
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
    .table-wrap {
      overflow-x: auto;
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
  isLoading = true;
  mostrarModal = false;

  nuevoCliente = '';
  nuevoTotal: number | null = null;

  pedidos: Pedido[] = [
    { id: 1, cliente: 'Marco Parra', estado: 'CREADO', total: 12990 },
    { id: 2, cliente: 'Yerson Herrera', estado: 'ACEPTADO', total: 8500 },
    { id: 3, cliente: 'Cliente Demo', estado: 'ENTREGADO', total: 21000 },
  ];

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

    const nuevoId = Math.max(0, ...this.pedidos.map((p) => p.id)) + 1;
    this.pedidos = [
      ...this.pedidos,
      { id: nuevoId, cliente: this.nuevoCliente, estado: 'CREADO', total: this.nuevoTotal },
    ];

    this.cerrarModal();
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.nuevoCliente = '';
    this.nuevoTotal = null;
  }
}
