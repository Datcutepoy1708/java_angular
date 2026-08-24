package com.store.service.impl;

import com.store.dto.request.attribute.AttributeRequest;
import com.store.dto.response.attribute.AttributeResponse;
import com.store.entity.category.Category;
import com.store.entity.product.Attribute;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.AttributeRepository;
import com.store.repository.CategoryRepository;
import com.store.service.AttributeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeServiceImpl implements AttributeService {

    private final AttributeRepository attributeRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoryAttributes", key = "#categoryId")
    public List<AttributeResponse> getAttributesByCategory(Integer categoryId) {
        log.info("Fetching attributes for category id {} from database (cache miss)", categoryId);
        List<Attribute> attributes = attributeRepository.findByCategoryCategoryIdOrderBySortOrderAscAttributeIdAsc(categoryId);
        return attributes.stream()
                .map(AttributeResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponse> getAttributesByCategories(List<Integer> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        return attributeRepository.findByCategoryCategoryIdInOrderBySortOrderAsc(categoryIds)
                .stream()
                .map(AttributeResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponse getAttributeById(Integer attributeId) {
        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thuộc tính với ID: " + attributeId));
        return AttributeResponse.fromEntity(attribute);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "categoryAttributes", allEntries = true)
    public AttributeResponse createAttribute(AttributeRequest request) {
        log.info("Creating attribute '{}' for category id {}", request.getName(), request.getCategoryId());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + request.getCategoryId()));

        String name = request.getName().trim();
        if (attributeRepository.existsByCategoryCategoryIdAndNameIgnoreCase(request.getCategoryId(), name)) {
            throw new DuplicateResourceException("Thuộc tính '" + name + "' đã tồn tại trong danh mục này");
        }

        Attribute attribute = Attribute.builder()
                .category(category)
                .name(name)
                .dataType(request.getDataType())
                .unit(request.getUnit() != null ? request.getUnit().trim() : null)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        Attribute saved = attributeRepository.save(attribute);
        return AttributeResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"categoryAttributes", "products", "productDetail"}, allEntries = true)
    public AttributeResponse updateAttribute(Integer attributeId, AttributeRequest request) {
        log.info("Updating attribute id {}", attributeId);

        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thuộc tính với ID: " + attributeId));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + request.getCategoryId()));

        String name = request.getName().trim();
        if (!attribute.getName().equalsIgnoreCase(name) &&
                attributeRepository.existsByCategoryCategoryIdAndNameIgnoreCase(request.getCategoryId(), name)) {
            throw new DuplicateResourceException("Thuộc tính '" + name + "' đã tồn tại trong danh mục này");
        }

        attribute.setCategory(category);
        attribute.setName(name);
        attribute.setDataType(request.getDataType());
        attribute.setUnit(request.getUnit() != null ? request.getUnit().trim() : null);
        if (request.getSortOrder() != null) {
            attribute.setSortOrder(request.getSortOrder());
        }

        Attribute updated = attributeRepository.save(attribute);
        return AttributeResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"categoryAttributes", "products", "productDetail"}, allEntries = true)
    public void deleteAttribute(Integer attributeId) {
        log.info("Deleting attribute id {}", attributeId);
        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thuộc tính với ID: " + attributeId));
        attributeRepository.delete(attribute);
    }
}
