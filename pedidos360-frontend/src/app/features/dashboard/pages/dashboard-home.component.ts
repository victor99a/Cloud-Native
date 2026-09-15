import { Component } from '@angular/core';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [NavbarComponent],
  template: `
    <app-navbar />

    <section class="page">
      <h1>Pedidos360</h1>
      <p>Resumen general (placeholder). Aquí irá el resumen según el rol del usuario.</p>

      <div class="cards">
        <div class="card">
          <span class="card-label">Pedidos activos</span>
          <span class="card-value">3</span>
        </div>
        <div class="card">
          <span class="card-label">Productos en catálogo</span>
          <span class="card-value">3</span>
        </div>
        <div class="card">
          <span class="card-label">Ventas del día</span>
          <span class="card-value">$42.490</span>
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
export class DashboardHomeComponent {}
