export type EstadoPedido =
  | 'CREADO'
  | 'ACEPTADO'
  | 'EN_PREPARACION'
  | 'DESPACHADO'
  | 'ENTREGADO'
  | 'CANCELADO';

export interface Pedido {
  id: number;
  cliente: string;
  estado: EstadoPedido;
  total: number;
}
