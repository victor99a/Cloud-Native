import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ProductosStoreService } from '../services/productos-store.service';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { ProductFormComponent, ProductoFormValue } from '../components/product-form.component';

@Component({
  selector: 'app-productos-list',
  standalone: true,
  imports: [DecimalPipe, ButtonComponent, ModalComponent, NavbarComponent, ProductFormComponent],
  template: `
    <app-navbar />

    <section class="page">
      <div class="header-row">
        <div>
          <h1>Productos</h1>
          <p>Datos de ejemplo (todavía no vienen del backend).</p>
        </div>
        <app-button label="Crear producto" (clicked)="mostrarModal = true" />
      </div>

      @if (productos().length === 0) {
        <p>No hay productos todavía.</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Nombre</th>
                <th>Precio</th>
                <th>Stock</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (producto of productos(); track producto.id) {
                <tr>
                  <td>{{ producto.id }}</td>
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

      <app-modal title="Crear producto" [isOpen]="mostrarModal" (close)="mostrarModal = false">
        <app-product-form
          (guardar)="crearProducto($event)"
          (cancelar)="mostrarModal = false"
        />
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
export class ProductosListComponent {
  protected readonly store = inject(ProductosStoreService);

  mostrarModal = false;
  productos = this.store.productos;

  crearProducto(valor: ProductoFormValue): void {
    this.store.crear(valor.nombre, valor.precio, valor.stock);
    this.mostrarModal = false;
  }
}
