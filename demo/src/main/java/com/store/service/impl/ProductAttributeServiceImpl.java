package com.store.service.impl;

import com.store.dto.request.attribute.BatchSaveProductAttributesRequest;
import com.store.dto.request.attribute.ProductAttributeValueRequest;
import com.store.dto.response.attribute.ProductAttributeValueResponse;
import com.store.entity.product.Attribute;
import com.store.entity.product.Product;
import com.store.entity.product.ProductAttributeValue;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.AttributeRepository;
import com.store.repository.ProductAttributeValueRepository;
import com.store.repository.ProductRepository;
import com.store.service.ProductAttributeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAttributeServiceImpl implements ProductAttributeService {

    private final ProductRepository productRepository;
    private final AttributeRepository attributeRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeValueResponse> getProductAttributes(Long productId) {
        log.info("Fetching attributes for product id {}", productId);
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId);
        }
        List<ProductAttributeValue> list = productAttributeValueRepository.findByProductProductId(productId);
        return list.stream()
                .map(ProductAttributeValueResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public List<ProductAttributeValueResponse> saveProductAttributes(Long productId, BatchSaveProductAttributesRequest request) {
        log.info("Saving attributes batch for product id {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        // Delete existing attributes for this product to replace with new batch
        productAttributeValueRepository.deleteByProductId(productId);

        List<ProductAttributeValue> toSave = new ArrayList<>();
        if (request != null && request.getAttributes() != null) {
            for (ProductAttributeValueRequest item : request.getAttributes()) {
                if (item.getValue() == null || item.getValue().trim().isEmpty()) {
                    continue; // skip empty values
                }
                Attribute attribute = attributeRepository.findById(item.getAttributeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thuộc tính với ID: " + item.getAttributeId()));

                ProductAttributeValue pav = ProductAttributeValue.builder()
                        .product(product)
                        .attribute(attribute)
                        .value(item.getValue().trim())
                        .build();
                toSave.add(pav);
            }
        }

        List<ProductAttributeValue> saved = productAttributeValueRepository.saveAll(toSave);
        return saved.stream()
                .map(ProductAttributeValueResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public void deleteProductAttribute(Long productId, Integer attributeId) {
        log.info("Deleting attribute id {} for product id {}", attributeId, productId);
        productAttributeValueRepository.deleteByProductIdAndAttributeId(productId, attributeId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public void deleteAllProductAttributes(Long productId) {
        log.info("Deleting all attributes for product id {}", productId);
        productAttributeValueRepository.deleteByProductId(productId);
    }
}
