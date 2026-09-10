package com.pedidos360.bff.service.impl;

import com.pedidos360.bff.dto.request.ProductoRequest;
import com.pedidos360.bff.dto.response.ProductoResponse;
import com.pedidos360.bff.entity.Producto;
import com.pedidos360.bff.exception.ResourceNotFoundException;
import com.pedidos360.bff.repository.ProductoRepository;
import com.pedidos360.bff.service.ProductoService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> findAll() {
        return productoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    @Transactional
    public ProductoResponse create(ProductoRequest request) {
        Producto producto = Producto.builder()
                .sku(request.sku())
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .precio(request.precio())
                .stock(request.stock())
                .activo(request.activo() != null ? request.activo() : true)
                .build();
        return toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse update(Long id, ProductoRequest request) {
        Producto producto = findEntity(id);
        producto.setSku(request.sku());
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        if (request.activo() != null) {
            producto.setActivo(request.activo());
        }
        return toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Producto producto = findEntity(id);
        productoRepository.delete(producto);
    }

    private Producto findEntity(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
    }

    private ProductoResponse toResponse(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getSku(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getActivo()
        );
    }
}
