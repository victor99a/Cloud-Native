import { Component, computed, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { PedidosStoreService } from '../../pedidos/services/pedidos-store.service';
import { ProductosStoreService } from '../../productos/services/productos-store.service';

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [DecimalPipe, NavbarComponent],
  template: `
    <app-navbar />

    <section class="page">
      <h1>Pedidos360</h1>
      <p>Resumen general (placeholder). Aquí irá el resumen según el rol del usuario.</p>

      <div class="cards">
        <div class="card">
          <span class="card-label">Pedidos activos</span>
          <span class="card-value">{{ pedidosActivos() }}</span>
        </div>
        <div class="card">
          <span class="card-label">Productos en catálogo</span>
          <span class="card-value">{{ totalProductos() }}</span>
        </div>
        <div class="card">
          <span class="card-label">Ventas entregadas</span>
          <span class="card-value">&#36;{{ ventasEntregadas() | number: '1.0-0' }}</span>
        </div>
      </div>
    </section>
  `,
  styles: `
    .page {
      max-width: 960px;
      margin: 0 auto;
      padding: 0 24px 40px;
    }
    .cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      margin-top: 20px;
    }
    .card {
      background: white;
      border-radius: 10px;
      padding: 20px;
      box-shadow: 0 1px 3px rgba(15, 15, 35, 0.08);
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .card-label {
      font-size: 13px;
      color: #6b7086;
    }
    .card-value {
      font-size: 28px;
      font-weight: 700;
      color: #1f2333;
    }
  `,
})
export class DashboardHomeComponent {
  private readonly pedidosStore = inject(PedidosStoreService);
  private readonly productosStore = inject(ProductosStoreService);

  private readonly pedidos = this.pedidosStore.pedidos;
  private readonly productos = this.productosStore.productos;

  pedidosActivos = computed(
    () => this.pedidos().filter((p) => p.estado !== 'ENTREGADO' && p.estado !== 'CANCELADO').length,
  );

  totalProductos = computed(() => this.productos().length);

  ventasEntregadas = computed(() =>
    this.pedidos()
      .filter((p) => p.estado === 'ENTREGADO')
      .reduce((suma, p) => suma + p.total, 0),
  );
}
