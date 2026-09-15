export interface Producto {
  id: number;
  sku: string;
  nombre: string;
  descripcion: string | null;
  precio: number;
  stock: number;
  activo: boolean;
}

export interface ProductoRequest {
  sku: string;
  nombre: string;
  descripcion?: string;
  precio: number;
  stock: number;
  activo?: boolean;
}
