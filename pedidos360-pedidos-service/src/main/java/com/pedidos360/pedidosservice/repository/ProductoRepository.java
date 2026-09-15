package com.pedidos360.pedidosservice.repository;

import com.pedidos360.pedidosservice.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Uso de solo-lectura: el servicio de pedidos consulta productos únicamente
 * para resolver el precio al calcular subtotales.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
