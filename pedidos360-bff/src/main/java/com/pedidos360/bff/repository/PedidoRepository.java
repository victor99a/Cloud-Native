package com.pedidos360.bff.repository;

import com.pedidos360.bff.entity.Pedido;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByNumero(String numero);

    boolean existsByNumero(String numero);
}
