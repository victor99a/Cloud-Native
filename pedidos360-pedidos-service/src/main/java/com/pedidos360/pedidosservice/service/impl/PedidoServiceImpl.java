package com.pedidos360.pedidosservice.service.impl;

import com.pedidos360.pedidosservice.dto.request.PedidoRequest;
import com.pedidos360.pedidosservice.dto.response.PedidoResponse;
import com.pedidos360.pedidosservice.entity.EstadoPedido;
import com.pedidos360.pedidosservice.entity.Pedido;
import com.pedidos360.pedidosservice.entity.PedidoItem;
import com.pedidos360.pedidosservice.entity.Producto;
import com.pedidos360.pedidosservice.exception.ResourceNotFoundException;
import com.pedidos360.pedidosservice.messaging.PedidoEventKafkaProducer;
import com.pedidos360.pedidosservice.messaging.PedidoEventPublisher;
import com.pedidos360.pedidosservice.messaging.TipoEventoPedido;
import com.pedidos360.pedidosservice.repository.PedidoRepository;
import com.pedidos360.pedidosservice.repository.ProductoRepository;
import com.pedidos360.pedidosservice.service.PedidoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final PedidoEventPublisher eventPublisher;
    private final PedidoEventKafkaProducer kafkaEventProducer;

    public PedidoServiceImpl(
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository,
            PedidoEventPublisher eventPublisher,
            PedidoEventKafkaProducer kafkaEventProducer) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.eventPublisher = eventPublisher;
        this.kafkaEventProducer = kafkaEventProducer;
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
        PedidoResponse response = toResponse(pedidoRepository.save(pedido));
        despuesDelCommit(() -> {
            eventPublisher.publishPedidoCreado(response);
            kafkaEventProducer.publishPedidoEvent(TipoEventoPedido.PEDIDO_CREADO, response);
        });
        return response;
    }

    @Override
    @Transactional
    public PedidoResponse updateEstado(Long id, EstadoPedido estado) {
        Pedido pedido = findEntity(id);
        pedido.setEstado(estado);
        PedidoResponse response = toResponse(pedidoRepository.save(pedido));
        despuesDelCommit(() -> {
            eventPublisher.publishEstadoCambiado(response);
            kafkaEventProducer.publishPedidoEvent(TipoEventoPedido.ESTADO_ACTUALIZADO, response);
        });
        return response;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Pedido pedido = findEntity(id);
        PedidoResponse snapshot = toResponse(pedido);
        pedidoRepository.delete(pedido);
        // La analítica descuenta el pedido eliminado de su estado (métricas coherentes).
        despuesDelCommit(() -> kafkaEventProducer.publishPedidoEvent(TipoEventoPedido.PEDIDO_ELIMINADO, snapshot));
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

    /**
     * Ejecuta la publicación de eventos solo si la transacción se confirma (afterCommit).
     * Evita anunciar pedidos que finalmente no se guardaron (rollback) y que los
     * consumidores lean un pedido que aún no es visible en la base de datos.
     * Sin transacción activa (p. ej. pruebas unitarias) publica de inmediato.
     */
    private static void despuesDelCommit(Runnable publicacion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publicacion.run();
                }
            });
        } else {
            publicacion.run();
        }
    }
}
