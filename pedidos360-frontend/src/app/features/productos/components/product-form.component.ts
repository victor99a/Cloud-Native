import { Component, EventEmitter, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ProductoRequest } from '../models/producto.model';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [FormsModule, ButtonComponent],
  template: `
    <form (ngSubmit)="onSubmit()">
      <label>
        SKU
        <input type="text" name="sku" [(ngModel)]="sku" required />
      </label>

      <label>
        Nombre
        <input type="text" name="nombre" [(ngModel)]="nombre" required />
      </label>

      <label>
        Precio
        <input type="number" name="precio" [(ngModel)]="precio" required min="0" />
      </label>

      <label>
        Stock
        <input type="number" name="stock" [(ngModel)]="stock" required min="0" />
      </label>

      <div class="form-actions">
        <app-button label="Cancelar" variant="secondary" (clicked)="cancelar.emit()" />
        <button type="submit" class="submit-btn">Guardar</button>
      </div>
    </form>
  `,
  styles: `
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
export class ProductFormComponent {
  @Output() guardar = new EventEmitter<ProductoRequest>();
  @Output() cancelar = new EventEmitter<void>();

  sku = '';
  nombre = '';
  precio: number | null = null;
  stock: number | null = null;

  onSubmit(): void {
    if (!this.sku || !this.nombre || this.precio === null || this.stock === null) {
      return;
    }
    this.guardar.emit({ sku: this.sku, nombre: this.nombre, precio: this.precio, stock: this.stock });
    this.sku = '';
    this.nombre = '';
    this.precio = null;
    this.stock = null;
  }
}
