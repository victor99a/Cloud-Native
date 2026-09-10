package com.pedidos360.bff.service.impl;

import com.pedidos360.bff.dto.request.PedidoRequest;
import com.pedidos360.bff.dto.response.PedidoResponse;
import com.pedidos360.bff.entity.EstadoPedido;
import com.pedidos360.bff.entity.Pedido;
import com.pedidos360.bff.entity.PedidoItem;
import com.pedidos360.bff.entity.Producto;
import com.pedidos360.bff.exception.ResourceNotFoundException;
import com.pedidos360.bff.repository.PedidoRepository;
import com.pedidos360.bff.repository.ProductoRepository;
import com.pedidos360.bff.service.PedidoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    public PedidoServiceImpl(PedidoRepository pedidoRepository, ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> findAll() {
        return pedidoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    @Transactional
    public PedidoResponse create(PedidoRequest request) {
        Pedido pedido = Pedido.builder()
                .numero(generarNumero())
                .cliente(request.cliente())
                .fecha(LocalDateTime.now())
                .estado(EstadoPedido.PENDIENTE)
                .build();

        for (PedidoRequest.PedidoItemRequest itemRequest : request.items()) {
            Producto producto = productoRepository.findById(itemRequest.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", itemRequest.productoId()));

            BigDecimal subtotal = producto.getPrecio()
                    .multiply(BigDecimal.valueOf(itemRequest.cantidad()));

            PedidoItem item = PedidoItem.builder()
                    .producto(producto)
                    .cantidad(itemRequest.cantidad())
                    .precioUnitario(producto.getPrecio())
                    .subtotal(subtotal)
                    .build();

            pedido.addItem(item);
        }

        pedido.recalculateTotal();
        return toResponse(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoResponse updateEstado(Long id, EstadoPedido estado) {
        Pedido pedido = findEntity(id);
        pedido.setEstado(estado);
        return toResponse(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Pedido pedido = findEntity(id);
        pedidoRepository.delete(pedido);
    }

    private Pedido findEntity(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
    }

    private String generarNumero() {
        long count = pedidoRepository.count() + 1;
        return String.format("PED-%06d", count);
    }

    private PedidoResponse toResponse(Pedido pedido) {
        List<PedidoResponse.ItemResponse> items = pedido.getItems().stream()
                .map(item -> new PedidoResponse.ItemResponse(
                        item.getProducto().getId(),
                        item.getProducto().getNombre(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.getSubtotal()
                ))
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getNumero(),
                pedido.getCliente(),
                pedido.getFecha(),
                pedido.getEstado(),
                pedido.getTotal(),
                items
        );
    }
}
