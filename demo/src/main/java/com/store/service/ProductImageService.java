package com.store.service;

import com.store.dto.request.ProductImageRequest;
import com.store.dto.response.ProductImageResponse;

import java.util.List;

public interface ProductImageService {

    /** Returns only active (non-deleted) images */
    List<ProductImageResponse> getImagesByProductId(Long productId);

    /** Returns only soft-deleted images for the product */
    List<ProductImageResponse> getDeletedImagesByProductId(Long productId);

    List<ProductImageResponse> getImagesByVariantId(Long variantId);

    ProductImageResponse addImage(Long productId, ProductImageRequest request);

    ProductImageResponse setMainImage(Long imageId);

    ProductImageResponse updateImage(Long imageId, ProductImageRequest request);

    /** Soft-delete: sets deleted_at = NOW() */
    void softDeleteImage(Long imageId);

    /** Restore: sets deleted_at = NULL */
    void restoreImage(Long imageId);

    void updateSortOrders(Long productId, List<Long> imageIdsInOrder);

    /** @deprecated Use softDeleteImage instead */
    @Deprecated
    void deleteImage(Long imageId);
}
