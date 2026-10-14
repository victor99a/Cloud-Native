import { Component, OnInit, computed, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { LoaderComponent } from '../../../shared/components/loader/loader.component';
import { AuthService } from '../../../core/auth/auth.service';
import { PedidosStoreService } from '../../pedidos/services/pedidos-store.service';
import { ProductosStoreService } from '../../productos/services/productos-store.service';

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [DecimalPipe, NavbarComponent, LoaderComponent],
  template: `
    <app-navbar />

    <section class="page">
      <header class="page-header">
        <div>
          <h1 class="page-title">Hola, {{ nombre() }}</h1>
          <p class="page-subtitle">Resumen general de Pedidos360.</p>
        </div>
      </header>

      <app-loader [isLoading]="cargando()" />

      @if (error(); as err) {
        <div class="error-message" role="alert">{{ err }}</div>
      }

      @if (!cargando()) {
        <div class="cards">
          <div class="card stat-card">
            <span class="stat-icon stat-icon--orders material-symbols-outlined" aria-hidden="true">assignment</span>
            <span class="card-label">Pedidos activos</span>
            <span class="card-value">{{ pedidosActivos() }}</span>
          </div>

          <div class="card stat-card">
            <span class="stat-icon stat-icon--products material-symbols-outlined" aria-hidden="true">inventory_2</span>
            <span class="card-label">Productos en catálogo</span>
            <span class="card-value">{{ totalProductos() }}</span>
          </div>

          <div class="card stat-card">
            <span class="stat-icon stat-icon--sales material-symbols-outlined" aria-hidden="true">payments</span>
            <span class="card-label">Ventas entregadas</span>
            <span class="card-value">&#36;{{ ventasEntregadas() | number: '1.0-0' }}</span>
          </div>
        </div>
      }
    </section>
  `,
  styles: `
    .cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 20px;
      margin-top: 8px;
    }

    .stat-card {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      transition:
        transform var(--transition),
        box-shadow var(--transition);
    }

    .stat-card:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-md);
    }

    .stat-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 44px;
      height: 44px;
      border-radius: var(--radius);
      font-size: 22px;
      margin-bottom: 6px;
    }

    .stat-icon--orders {
      background: var(--color-primary-soft);
      color: var(--color-primary);
    }

    .stat-icon--products {
      background: var(--color-teal-soft);
      color: var(--color-teal);
    }

    .stat-icon--sales {
      background: var(--color-success-soft);
      color: var(--color-success);
    }

    .card-label {
      font-size: 13px;
      font-weight: 500;
      color: var(--color-text-muted);
    }

    .card-value {
      font-size: clamp(24px, 4vw, 30px);
      font-weight: 700;
      color: var(--color-text);
      letter-spacing: -0.02em;
    }
  `,
})
export class DashboardHomeComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly pedidosStore = inject(PedidosStoreService);
  private readonly productosStore = inject(ProductosStoreService);

  private readonly pedidos = this.pedidosStore.pedidos;
  private readonly productos = this.productosStore.productos;

  ngOnInit(): void {
    this.pedidosStore.cargar();
    this.productosStore.cargar();
  }

  nombre(): string {
    return this.authService.userName || 'usuario';
  }

  cargando = computed(
    () => this.pedidosStore.isLoading() || this.productosStore.isLoading(),
  );

  error = computed(() => this.pedidosStore.error() ?? this.productosStore.error());

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
