import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { ErrorHandlerService } from '../../../core/services/error-handler.service';
import { Producto, ProductoRequest } from '../models/producto.model';

@Injectable({ providedIn: 'root' })
export class ProductosStoreService {
  private readonly api = inject(ApiService);
  private readonly errorHandler = inject(ErrorHandlerService);

  private readonly _productos = signal<Producto[]>([]);
  private readonly _isLoading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly productos = this._productos.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  /** Error de una acción (crear / ajustar stock): se muestra sin ocultar el catálogo. */
  private readonly _accionError = signal<string | null>(null);
  /** true mientras se crea un producto. */
  private readonly _guardando = signal(false);
  /** Id del producto cuyo stock se está actualizando (evita clics repetidos). */
  private readonly _ajustandoId = signal<number | null>(null);

  readonly accionError = this._accionError.asReadonly();
  readonly guardando = this._guardando.asReadonly();
  readonly ajustandoId = this._ajustandoId.asReadonly();

  limpiarAccionError(): void {
    this._accionError.set(null);
  }

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
    if (this._guardando()) {
      return;
    }
    this._guardando.set(true);
    this._accionError.set(null);
    this.api.post<Producto>('/productos', { ...request, activo: request.activo ?? true }).subscribe({
      next: (nuevo) => {
        this._productos.update((actuales) => [...actuales, nuevo]);
        this._guardando.set(false);
      },
      error: (err) => {
        this._accionError.set(`No se pudo crear el producto. ${this.errorHandler.handle(err)}.`);
        this._guardando.set(false);
      },
    });
  }

  ajustarStock(id: number, delta: number): void {
    const producto = this._productos().find((p) => p.id === id);
    if (!producto || this._ajustandoId() !== null) {
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
    this._ajustandoId.set(id);
    this._accionError.set(null);
    this.api.put<Producto>(`/productos/${id}`, request).subscribe({
      next: (actualizado) => {
        this._productos.update((actuales) => actuales.map((p) => (p.id === id ? actualizado : p)));
        this._ajustandoId.set(null);
      },
      error: (err) => {
        this._accionError.set(`No se pudo actualizar el stock. ${this.errorHandler.handle(err)}.`);
        this._ajustandoId.set(null);
      },
    });
  }
}
