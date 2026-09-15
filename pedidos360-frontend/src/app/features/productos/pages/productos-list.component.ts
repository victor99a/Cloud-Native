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
      <div class="header-row">
        <div>
          <h1>Productos</h1>
          <p>Datos reales, consumidos desde el API Gateway.</p>
        </div>
        <app-button label="Crear producto" (clicked)="mostrarModal = true" />
      </div>

      <app-loader [isLoading]="store.isLoading()" />

      @if (store.error(); as error) {
        <p class="error">{{ error }}</p>
      }

      @if (!store.isLoading()) {
        @if (store.productos().length === 0) {
          <p>No hay productos todavía.</p>
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
    .table-wrap {
      overflow-x: auto;
    }
    .badge {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 600;
    }
    .badge.disponible {
      background: #dcfce7;
      color: #16a34a;
    }
    .badge.agotado {
      background: #fee2e2;
      color: #dc2626;
    }
    .stock-actions {
      display: flex;
      gap: 6px;
    }
    .stock-btn {
      width: 28px;
      height: 28px;
      border-radius: 6px;
      border: 1px solid #e2e4ec;
      background: white;
      cursor: pointer;
      font-size: 14px;
      line-height: 1;
    }
    .stock-btn:hover {
      background: #f4f5f9;
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
