import { Component, OnInit, computed, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { LoaderComponent } from '../../../shared/components/loader/loader.component';
import { AnaliticaStoreService } from '../services/analitica-store.service';

@Component({
  selector: 'app-analitica-dashboard',
  standalone: true,
  imports: [DatePipe, DecimalPipe, NavbarComponent, LoaderComponent],
  template: `
    <app-navbar />

    <section class="page">
      <header class="page-header">
        <div>
          <h1 class="page-title">Analítica de pedidos</h1>
          <p class="page-subtitle">Métricas agregadas en tiempo real desde Kafka.</p>
        </div>
      </header>

      <app-loader [isLoading]="store.isLoading()" />

      @if (store.error(); as error) {
        <div class="error-message" role="alert">{{ error }}</div>
      }

      @if (!store.isLoading() && !store.error()) {
        <div class="cards">
          <div class="card stat-card">
            <span class="stat-icon stat-icon--orders material-symbols-outlined" aria-hidden="true">receipt_long</span>
            <span class="card-label">Total de pedidos</span>
            <span class="card-value">{{ totalPedidos() }}</span>
          </div>
          <div class="card stat-card">
            <span class="stat-icon stat-icon--sales material-symbols-outlined" aria-hidden="true">payments</span>
            <span class="card-label">Monto vendido</span>
            <span class="card-value">&#36;{{ totalVendido() | number: '1.0-0' }}</span>
          </div>
          <div class="card stat-card">
            <span class="stat-icon stat-icon--states material-symbols-outlined" aria-hidden="true">category</span>
            <span class="card-label">Estados registrados</span>
            <span class="card-value">{{ store.metricas().length }}</span>
          </div>
        </div>

        @if (store.metricas().length === 0) {
          <div class="empty-state">
            <div class="empty-icon material-symbols-outlined" aria-hidden="true">monitoring</div>
            <p>Aún no hay eventos procesados. Crea un pedido para empezar a ver métricas.</p>
          </div>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Estado</th>
                  <th>Pedidos</th>
                  <th>Monto vendido</th>
                  <th>Última actualización</th>
                </tr>
              </thead>
              <tbody>
                @for (metrica of store.metricas(); track metrica.estado) {
                  <tr>
                    <td>{{ metrica.estado }}</td>
                    <td>{{ metrica.totalPedidos }}</td>
                    <td>&#36;{{ metrica.totalVendido | number: '1.0-0' }}</td>
                    <td>{{ metrica.ultimaActualizacion | date: 'short' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      }
    </section>
  `,
  styles: `
    .cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 20px;
      margin-bottom: 20px;
    }

    .stat-card {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 10px;
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

    .stat-icon--sales {
      background: var(--color-success-soft);
      color: var(--color-success);
    }

    .stat-icon--states {
      background: var(--color-teal-soft);
      color: var(--color-teal);
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

    .table-wrap {
      margin-top: 8px;
    }
  `,
})
export class AnaliticaDashboardComponent implements OnInit {
  readonly store = inject(AnaliticaStoreService);

  ngOnInit(): void {
    this.store.cargar();
  }

  totalPedidos = computed(() => this.store.metricas().reduce((suma, m) => suma + m.totalPedidos, 0));

  totalVendido = computed(() => this.store.metricas().reduce((suma, m) => suma + m.totalVendido, 0));
}
