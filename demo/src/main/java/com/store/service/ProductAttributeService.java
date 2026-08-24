package com.store.service;

import com.store.dto.request.attribute.BatchSaveProductAttributesRequest;
import com.store.dto.response.attribute.ProductAttributeValueResponse;

import java.util.List;

public interface ProductAttributeService {

    List<ProductAttributeValueResponse> getProductAttributes(Long productId);

    List<ProductAttributeValueResponse> saveProductAttributes(Long productId, BatchSaveProductAttributesRequest request);

    void deleteProductAttribute(Long productId, Integer attributeId);

    void deleteAllProductAttributes(Long productId);
}
