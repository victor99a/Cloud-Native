import { Injectable, signal } from '@angular/core';
import { Producto } from '../models/producto.model';

@Injectable({ providedIn: 'root' })
export class ProductosStoreService {
  private readonly _productos = signal<Producto[]>([
    { id: 1, nombre: 'Fideos', precio: 2900, stock: 15 },
    { id: 2, nombre: 'Bebida 1.5L', precio: 1800, stock: 0 },
    { id: 3, nombre: 'Pan Amasado', precio: 2500, stock: 8 },
  ]);

  readonly productos = this._productos.asReadonly();

  crear(nombre: string, precio: number, stock: number): void {
    const nuevoId = Math.max(0, ...this._productos().map((p) => p.id)) + 1;
    this._productos.update((actuales) => [
      ...actuales,
      { id: nuevoId, nombre, precio, stock },
    ]);
  }

  ajustarStock(id: number, delta: number): void {
    this._productos.update((actuales) =>
      actuales.map((p) =>
        p.id === id ? { ...p, stock: Math.max(0, p.stock + delta) } : p,
      ),
    );
  }
}
