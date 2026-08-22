package com.store.service;

import com.store.dto.request.ProductImageRequest;
import com.store.dto.response.ProductImageResponse;

import java.util.List;

public interface ProductImageService {

    List<ProductImageResponse> getImagesByProductId(Long productId);

    List<ProductImageResponse> getImagesByVariantId(Long variantId);

    ProductImageResponse addImage(Long productId, ProductImageRequest request);

    ProductImageResponse setMainImage(Long imageId);

    ProductImageResponse updateImage(Long imageId, ProductImageRequest request);

    void deleteImage(Long imageId);

    void updateSortOrders(Long productId, List<Long> imageIdsInOrder);
}
