import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ProductosStoreService } from '../services/productos-store.service';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { LoaderComponent } from '../../../shared/components/loader/loader.component';
import { ProductFormComponent } from '../components/product-form.component';
import { ProductoRequest } from '../models/producto.model';

@Component({
  selector: 'app-productos-list',
  standalone: true,
  imports: [
    DecimalPipe,
    ButtonComponent,
    ModalComponent,
    NavbarComponent,
    LoaderComponent,
    ProductFormComponent,
  ],
  template: `
    <app-navbar />

    <section class="page">
      <header class="page-header">
        <div>
          <h1 class="page-title">Productos</h1>
          <p class="page-subtitle">Administra el catálogo y el stock.</p>
        </div>
        <app-button label="Crear producto" (clicked)="mostrarModal = true" />
      </header>

      <app-loader [isLoading]="store.isLoading()" />

      @if (store.error(); as error) {
        <div class="error-message" role="alert">{{ error }}</div>
      }

      @if (!store.isLoading() && !store.error()) {
        @if (store.productos().length === 0) {
          <div class="empty-state">
            <div class="empty-icon" aria-hidden="true">📦</div>
            <p>No hay productos todavía. Crea el primero.</p>
          </div>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Nombre</th>
                  <th>Precio</th>
                  <th>Stock</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (producto of store.productos(); track producto.id) {
                  <tr>
                    <td>{{ producto.sku }}</td>
                    <td>{{ producto.nombre }}</td>
                    <td>&#36;{{ producto.precio | number: '1.0-0' }}</td>
                    <td>
                      @if (producto.stock > 0) {
                        <span class="badge disponible">Disponible ({{ producto.stock }})</span>
                      } @else {
                        <span class="badge agotado">Agotado</span>
                      }
                    </td>
                    <td class="stock-actions">
                      <button
                        class="stock-btn"
                        type="button"
                        [attr.aria-label]="'Restar stock a ' + producto.nombre"
                        (click)="store.ajustarStock(producto.id, -1)"
                      >
                        −
                      </button>
                      <button
                        class="stock-btn"
                        type="button"
                        [attr.aria-label]="'Sumar stock a ' + producto.nombre"
                        (click)="store.ajustarStock(producto.id, 1)"
                      >
                        +
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      }

      <app-modal title="Crear producto" [isOpen]="mostrarModal" (close)="mostrarModal = false">
        <app-product-form (guardar)="crearProducto($event)" (cancelar)="mostrarModal = false" />
      </app-modal>
    </section>
  `,
  styles: `
    .badge {
      display: inline-block;
      padding: 4px 12px;
      border-radius: var(--radius-full);
      font-size: 12px;
      font-weight: 600;
      white-space: nowrap;
    }

    .badge.disponible {
      background: var(--color-success-soft);
      color: var(--color-success);
    }

    .badge.agotado {
      background: var(--color-danger-soft);
      color: var(--color-danger);
    }

    .stock-actions {
      display: flex;
      gap: 6px;
      justify-content: flex-end;
    }

    .stock-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--color-border);
      background: var(--color-surface);
      cursor: pointer;
      font-size: 16px;
      line-height: 1;
      color: var(--color-text);
      transition: background var(--transition);
    }

    .stock-btn:hover {
      background: var(--color-primary-soft);
      border-color: var(--color-primary);
      color: var(--color-primary);
    }
  `,
})
export class ProductosListComponent implements OnInit {
  readonly store = inject(ProductosStoreService);

  mostrarModal = false;

  ngOnInit(): void {
    this.store.cargar();
  }

  crearProducto(request: ProductoRequest): void {
    this.store.crear(request);
    this.mostrarModal = false;
  }
}
