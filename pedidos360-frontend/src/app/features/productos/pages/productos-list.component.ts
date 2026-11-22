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

      @if (store.accionError(); as accionError) {
        <div class="error-message action-error" role="alert">
          <span>{{ accionError }}</span>
          <button type="button" aria-label="Cerrar aviso" (click)="store.limpiarAccionError()">✕</button>
        </div>
      }

      @if (!store.isLoading() && !store.error()) {
        @if (store.productos().length === 0) {
          <div class="empty-state">
            <div class="empty-icon material-symbols-outlined" aria-hidden="true">inventory_2</div>
            <p>No hay productos todavía. Crea el primero.</p>
          </div>
        } @else {
          <div class="table-wrap">
            <table class="table-stack">
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
                    <td class="nowrap" data-label="SKU">{{ producto.sku }}</td>
                    <td data-label="Nombre">{{ producto.nombre }}</td>
                    <td class="nowrap" data-label="Precio">&#36;{{ producto.precio | number: '1.0-0' }}</td>
                    <td data-label="Stock">
                      @if (producto.stock > 0) {
                        <span class="badge disponible">Disponible ({{ producto.stock }})</span>
                      } @else {
                        <span class="badge agotado">Agotado</span>
                      }
                    </td>
                    <td class="stock-actions" data-label="">
                      <button
                        class="stock-btn"
                        type="button"
                        [attr.aria-label]="'Restar stock a ' + producto.nombre"
                        [disabled]="store.ajustandoId() !== null || producto.stock === 0"
                        (click)="store.ajustarStock(producto.id, -1)"
                      >
                        −
                      </button>
                      <button
                        class="stock-btn"
                        type="button"
                        [attr.aria-label]="'Sumar stock a ' + producto.nombre"
                        [disabled]="store.ajustandoId() !== null"
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
      color: var(--color-success-text);
    }

    .badge.agotado {
      background: var(--color-danger-soft);
      color: var(--color-danger-text);
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
      width: 40px;
      height: 40px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--color-border);
      background: var(--color-surface);
      cursor: pointer;
      font-size: 16px;
      line-height: 1;
      color: var(--color-text);
      transition: background var(--transition);
    }

    .stock-btn:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }

    .stock-btn:hover:not(:disabled) {
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
