package com.store.service;

import com.store.dto.request.attribute.AttributeRequest;
import com.store.dto.response.attribute.AttributeResponse;

import java.util.List;

public interface AttributeService {

    List<AttributeResponse> getAttributesByCategory(Integer categoryId);

    List<AttributeResponse> getAttributesByCategories(List<Integer> categoryIds);

    AttributeResponse getAttributeById(Integer attributeId);

    AttributeResponse createAttribute(AttributeRequest request);

    AttributeResponse updateAttribute(Integer attributeId, AttributeRequest request);

    void deleteAttribute(Integer attributeId);
}
