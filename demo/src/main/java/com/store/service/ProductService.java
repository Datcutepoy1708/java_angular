package com.store.service;

import com.store.dto.request.BulkActionRequest;
import com.store.dto.request.ProductFilterRequest;
import com.store.dto.request.ProductRequest;
import com.store.dto.response.BulkActionResult;
import com.store.dto.response.PageResponse;
import com.store.dto.response.ProductResponse;

public interface ProductService {

    PageResponse<ProductResponse> getProducts(ProductFilterRequest filter);

    PageResponse<ProductResponse> getDeletedProducts(int page, int size);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySlug(String slug);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    /** Soft-delete: sets deleted_at = NOW(), does NOT change status or cascade to variants/images */
    void softDeleteProduct(Long id);

    /** Restore: sets deleted_at = NULL */
    void restoreProduct(Long id);

    BulkActionResult bulkAction(BulkActionRequest request);

    void incrementViewCount(Long id);

    /** @deprecated Use softDeleteProduct instead */
    @Deprecated
    void deleteProduct(Long id);
}
