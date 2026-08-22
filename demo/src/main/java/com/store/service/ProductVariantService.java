package com.store.service;

import com.store.dto.request.ProductVariantRequest;
import com.store.dto.response.ProductVariantResponse;

import java.util.List;

public interface ProductVariantService {

    List<ProductVariantResponse> getVariantsByProductId(Long productId);

    ProductVariantResponse getVariantById(Long variantId);

    ProductVariantResponse getVariantBySku(String skuVariant);

    ProductVariantResponse createVariant(Long productId, ProductVariantRequest request);

    ProductVariantResponse updateVariant(Long variantId, ProductVariantRequest request);

    void deleteVariant(Long variantId);
}
