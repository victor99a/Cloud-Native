package com.pedidos360.bff.service;

import com.pedidos360.bff.dto.request.ProductoRequest;
import com.pedidos360.bff.dto.response.ProductoResponse;
import java.util.List;

public interface ProductoService {

    List<ProductoResponse> findAll();

    ProductoResponse findById(Long id);

    ProductoResponse create(ProductoRequest request);

    ProductoResponse update(Long id, ProductoRequest request);

    void delete(Long id);
}
