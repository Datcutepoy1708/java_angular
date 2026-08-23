package com.store.service;

import com.store.dto.request.ProductVariantRequest;
import com.store.dto.response.ProductVariantResponse;

import java.util.List;

public interface ProductVariantService {

    /** Returns only active (non-deleted) variants */
    List<ProductVariantResponse> getVariantsByProductId(Long productId);

    /** Returns only soft-deleted variants for the product */
    List<ProductVariantResponse> getDeletedVariantsByProductId(Long productId);

    ProductVariantResponse getVariantById(Long variantId);

    ProductVariantResponse getVariantBySku(String skuVariant);

    ProductVariantResponse createVariant(Long productId, ProductVariantRequest request);

    ProductVariantResponse updateVariant(Long variantId, ProductVariantRequest request);

    /** Soft-delete: sets deleted_at = NOW(), does NOT change status */
    void softDeleteVariant(Long variantId);

    /** Restore: sets deleted_at = NULL */
    void restoreVariant(Long variantId);

    /** @deprecated Use softDeleteVariant instead */
    @Deprecated
    void deleteVariant(Long variantId);
}
