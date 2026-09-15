import { Injectable, signal } from '@angular/core';
import { EstadoPedido, Pedido } from '../models/pedido.model';

// Orden válido del flujo de estados (Caso 0: no se puede "despachar" sin "aceptar").
const FLUJO_ESTADOS: EstadoPedido[] = [
  'CREADO',
  'ACEPTADO',
  'EN_PREPARACION',
  'DESPACHADO',
  'ENTREGADO',
];

@Injectable({ providedIn: 'root' })
export class PedidosStoreService {
  // signal() guarda un valor reactivo: cualquier componente que lo lea
  // se actualiza solo cuando cambia, sin que tengamos que avisarle manualmente.
  private readonly _pedidos = signal<Pedido[]>([
    { id: 1, cliente: 'Marco Parra', estado: 'CREADO', total: 12990 },
    { id: 2, cliente: 'Yerson Herrera', estado: 'ACEPTADO', total: 8500 },
    { id: 3, cliente: 'Cliente Demo', estado: 'ENTREGADO', total: 21000 },
  ]);

  readonly pedidos = this._pedidos.asReadonly();

  findById(id: number): Pedido | undefined {
    return this._pedidos().find((p) => p.id === id);
  }

  crear(cliente: string, total: number): void {
    const nuevoId = Math.max(0, ...this._pedidos().map((p) => p.id)) + 1;
    this._pedidos.update((actuales) => [
      ...actuales,
      { id: nuevoId, cliente, estado: 'CREADO', total },
    ]);
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
    this._pedidos.update((actuales) =>
      actuales.map((p) => (p.id === id ? { ...p, estado: siguiente } : p)),
    );
  }

  cancelar(id: number): void {
    this._pedidos.update((actuales) =>
      actuales.map((p) => (p.id === id ? { ...p, estado: 'CANCELADO' as EstadoPedido } : p)),
    );
  }
}
