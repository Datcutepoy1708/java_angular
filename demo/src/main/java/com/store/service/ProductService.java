package com.store.service;

import com.store.dto.request.ProductFilterRequest;
import com.store.dto.request.ProductRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.ProductResponse;

public interface ProductService {

    PageResponse<ProductResponse> getProducts(ProductFilterRequest filter);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySlug(String slug);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    void incrementViewCount(Long id);
}
