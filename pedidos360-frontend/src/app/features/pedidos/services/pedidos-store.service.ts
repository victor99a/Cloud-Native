import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { EstadoPedido, Pedido, PedidoRequest } from '../models/pedido.model';

// Orden válido del flujo de estados del backend (pedidos360-pedidos-service).
const FLUJO_ESTADOS: EstadoPedido[] = [
  'PENDIENTE',
  'CONFIRMADO',
  'EN_PREPARACION',
  'ENVIADO',
  'ENTREGADO',
];

@Injectable({ providedIn: 'root' })
export class PedidosStoreService {
  private readonly api = inject(ApiService);

  private readonly _pedidos = signal<Pedido[]>([]);
  private readonly _isLoading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly pedidos = this._pedidos.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  cargar(): void {
    this._isLoading.set(true);
    this._error.set(null);
    this.api.get<Pedido[]>('/pedidos').subscribe({
      next: (pedidos) => {
        this._pedidos.set(pedidos);
        this._isLoading.set(false);
      },
      error: () => {
        this._error.set('No se pudieron cargar los pedidos.');
        this._isLoading.set(false);
      },
    });
  }

  findById(id: number): Pedido | undefined {
    return this._pedidos().find((p) => p.id === id);
  }

  crear(request: PedidoRequest): void {
    this.api.post<Pedido>('/pedidos', request).subscribe({
      next: (nuevo) => this._pedidos.update((actuales) => [...actuales, nuevo]),
      error: () => this._error.set('No se pudo crear el pedido.'),
    });
  }

  /** Devuelve el siguiente estado válido, o null si ya no hay uno (ENTREGADO/CANCELADO). */
  siguienteEstado(estado: EstadoPedido): EstadoPedido | null {
    const index = FLUJO_ESTADOS.indexOf(estado);
    if (index === -1 || index === FLUJO_ESTADOS.length - 1) {
      return null;
    }
    return FLUJO_ESTADOS[index + 1];
  }

  avanzarEstado(id: number): void {
    const pedido = this.findById(id);
    if (!pedido) {
      return;
    }
    const siguiente = this.siguienteEstado(pedido.estado);
    if (!siguiente) {
      return;
    }
    this.cambiarEstado(id, siguiente);
  }

  cancelar(id: number): void {
    this.cambiarEstado(id, 'CANCELADO');
  }

  private cambiarEstado(id: number, estado: EstadoPedido): void {
    this.api.patch<Pedido>(`/pedidos/${id}/estado`, { estado }).subscribe({
      next: (actualizado) =>
        this._pedidos.update((actuales) =>
          actuales.map((p) => (p.id === id ? actualizado : p)),
        ),
      error: () => this._error.set('No se pudo cambiar el estado del pedido.'),
    });
  }
}
