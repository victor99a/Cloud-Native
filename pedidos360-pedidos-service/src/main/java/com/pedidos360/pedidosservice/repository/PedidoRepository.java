package com.pedidos360.pedidosservice.repository;

import com.pedidos360.pedidosservice.entity.Pedido;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByNumero(String numero);

    boolean existsByNumero(String numero);
}
