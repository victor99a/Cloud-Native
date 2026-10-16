import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { Producto, ProductoRequest } from '../models/producto.model';

@Injectable({ providedIn: 'root' })
export class ProductosStoreService {
  private readonly api = inject(ApiService);

  private readonly _productos = signal<Producto[]>([]);
  private readonly _isLoading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly productos = this._productos.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  cargar(): void {
    this._isLoading.set(true);
    this._error.set(null);
    this.api.get<Producto[]>('/productos').subscribe({
      next: (productos) => {
        this._productos.set(productos);
        this._isLoading.set(false);
      },
      error: () => {
        this._error.set('No se pudieron cargar los productos.');
        this._isLoading.set(false);
      },
    });
  }

  crear(request: ProductoRequest): void {
    this.api.post<Producto>('/productos', { ...request, activo: request.activo ?? true }).subscribe({
      next: (nuevo) => this._productos.update((actuales) => [...actuales, nuevo]),
      error: () => this._error.set('No se pudo crear el producto.'),
    });
  }

  ajustarStock(id: number, delta: number): void {
    const producto = this._productos().find((p) => p.id === id);
    if (!producto) {
      return;
    }
    const nuevoStock = Math.max(0, producto.stock + delta);
    const request: ProductoRequest = {
      sku: producto.sku,
      nombre: producto.nombre,
      descripcion: producto.descripcion ?? undefined,
      precio: producto.precio,
      stock: nuevoStock,
      activo: producto.activo,
    };
    this.api.put<Producto>(`/productos/${id}`, request).subscribe({
      next: (actualizado) =>
        this._productos.update((actuales) =>
          actuales.map((p) => (p.id === id ? actualizado : p)),
        ),
      error: () => this._error.set('No se pudo actualizar el stock.'),
    });
  }
}
