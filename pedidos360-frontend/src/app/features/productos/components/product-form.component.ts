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
        <input type="text" name="sku" [(ngModel)]="sku" required placeholder="PROD-001" />
      </label>

      <label>
        Nombre
        <input type="text" name="nombre" [(ngModel)]="nombre" required placeholder="Nombre del producto" />
      </label>

      <label>
        Descripción (opcional)
        <textarea
          name="descripcion"
          rows="2"
          [(ngModel)]="descripcion"
          placeholder="Breve descripción del producto"
        ></textarea>
      </label>

      <div class="row">
        <label>
          Precio
          <input type="number" name="precio" [(ngModel)]="precio" required min="0" step="0.01" />
        </label>

        <label>
          Stock
          <input type="number" name="stock" [(ngModel)]="stock" required min="0" step="1" />
        </label>
      </div>

      <label class="checkbox">
        <input type="checkbox" name="activo" [(ngModel)]="activo" />
        Producto activo
      </label>

      <div class="form-actions">
        <app-button label="Cancelar" variant="secondary" (clicked)="cancelar.emit()" />
        <app-button label="Guardar" type="submit" [disabled]="!formValido()" />
      </div>
    </form>
  `,
  styles: `
    form {
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }

    .checkbox {
      flex-direction: row;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: var(--color-text);
      cursor: pointer;
    }

    .checkbox input {
      width: 18px;
      height: 18px;
      accent-color: var(--color-primary);
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 4px;
    }

    @media (max-width: 480px) {
      .row {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class ProductFormComponent {
  @Output() guardar = new EventEmitter<ProductoRequest>();
  @Output() cancelar = new EventEmitter<void>();

  sku = '';
  nombre = '';
  descripcion = '';
  precio: number | null = null;
  stock: number | null = null;
  activo = true;

  formValido(): boolean {
    return this.sku.trim().length > 0 && this.nombre.trim().length > 0 && this.precio !== null && this.stock !== null;
  }

  onSubmit(): void {
    if (!this.formValido()) {
      return;
    }
    this.guardar.emit({
      sku: this.sku.trim(),
      nombre: this.nombre.trim(),
      descripcion: this.descripcion.trim() || undefined,
      precio: this.precio as number,
      stock: this.stock as number,
      activo: this.activo,
    });
    this.sku = '';
    this.nombre = '';
    this.descripcion = '';
    this.precio = null;
    this.stock = null;
    this.activo = true;
  }
}
