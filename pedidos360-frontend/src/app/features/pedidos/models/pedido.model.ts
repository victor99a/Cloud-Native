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
