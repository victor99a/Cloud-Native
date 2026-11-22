export type EstadoPedido =
  | 'PENDIENTE'
  | 'CONFIRMADO'
  | 'EN_PREPARACION'
  | 'ENVIADO'
  | 'ENTREGADO'
  | 'CANCELADO';

export interface PedidoItem {
  productoId: number;
  nombreProducto: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Pedido {
  id: number;
  numero: string;
  cliente: string;
  fecha: string;
  estado: EstadoPedido;
  total: number;
  items: PedidoItem[];
}

export interface PedidoItemRequest {
  productoId: number;
  cantidad: number;
}

export interface PedidoRequest {
  cliente: string;
  items: PedidoItemRequest[];
}

/** Nombre legible de cada estado para mostrar en la interfaz. */
export const ESTADO_LABELS: Record<EstadoPedido, string> = {
  PENDIENTE: 'Pendiente',
  CONFIRMADO: 'Confirmado',
  EN_PREPARACION: 'En preparación',
  ENVIADO: 'Enviado',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
};

/** Devuelve la etiqueta legible de un estado (o el valor original si no se reconoce). */
export function estadoLabel(estado: string): string {
  return ESTADO_LABELS[estado as EstadoPedido] ?? estado;
}
