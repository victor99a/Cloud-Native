package com.pedidos360.productosservice.service;

import com.pedidos360.productosservice.dto.request.ProductoRequest;
import com.pedidos360.productosservice.dto.response.ProductoResponse;
import java.util.List;

public interface ProductoService {

    List<ProductoResponse> findAll();

    ProductoResponse findById(Long id);

    ProductoResponse create(ProductoRequest request);

    ProductoResponse update(Long id, ProductoRequest request);

    void delete(Long id);
}
