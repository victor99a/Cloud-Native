package com.pedidos360.pedidosservice.service;

import com.pedidos360.pedidosservice.dto.request.PedidoRequest;
import com.pedidos360.pedidosservice.dto.response.PedidoResponse;
import com.pedidos360.pedidosservice.entity.EstadoPedido;
import java.util.List;

public interface PedidoService {

    List<PedidoResponse> findAll();

    PedidoResponse findById(Long id);

    PedidoResponse create(PedidoRequest request);

    PedidoResponse updateEstado(Long id, EstadoPedido estado);

    void delete(Long id);
}
