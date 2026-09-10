import { Component } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Producto } from '../models/producto.model';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-productos-list',
  standalone: true,
  imports: [DecimalPipe, FormsModule, ButtonComponent, ModalComponent, NavbarComponent],
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

      @if (productos.length === 0) {
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
              </tr>
            </thead>
            <tbody>
              @for (producto of productos; track producto.id) {
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
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      <app-modal title="Crear producto" [isOpen]="mostrarModal" (close)="cerrarModal()">
        <form (ngSubmit)="crearProducto()">
          <label>
            Nombre
            <input type="text" name="nombre" [(ngModel)]="nuevoNombre" required />
          </label>

          <label>
            Precio
            <input type="number" name="precio" [(ngModel)]="nuevoPrecio" required min="0" />
          </label>

          <label>
            Stock
            <input type="number" name="stock" [(ngModel)]="nuevoStock" required min="0" />
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
export class ProductosListComponent {
  mostrarModal = false;

  nuevoNombre = '';
  nuevoPrecio: number | null = null;
  nuevoStock: number | null = null;

  productos: Producto[] = [
    { id: 1, nombre: 'Fideos', precio: 2900, stock: 15 },
    { id: 2, nombre: 'Bebida 1.5L', precio: 1800, stock: 0 },
    { id: 3, nombre: 'Pan Amasado', precio: 2500, stock: 8 },
  ];

  crearProducto(): void {
    if (!this.nuevoNombre || this.nuevoPrecio === null || this.nuevoStock === null) {
      return;
    }

    const nuevoId = Math.max(0, ...this.productos.map((p) => p.id)) + 1;
    this.productos = [
      ...this.productos,
      {
        id: nuevoId,
        nombre: this.nuevoNombre,
        precio: this.nuevoPrecio,
        stock: this.nuevoStock,
      },
    ];

    this.cerrarModal();
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.nuevoNombre = '';
    this.nuevoPrecio = null;
    this.nuevoStock = null;
  }
}
