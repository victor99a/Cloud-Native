package com.pedidos360.bff.service;

import com.pedidos360.bff.dto.request.PedidoRequest;
import com.pedidos360.bff.dto.response.PedidoResponse;
import com.pedidos360.bff.entity.EstadoPedido;
import java.util.List;

public interface PedidoService {

    List<PedidoResponse> findAll();

    PedidoResponse findById(Long id);

    PedidoResponse create(PedidoRequest request);

    PedidoResponse updateEstado(Long id, EstadoPedido estado);

    void delete(Long id);
}
