package com.store.service.impl;

import com.store.dto.request.ProductVariantRequest;
import com.store.dto.response.ProductVariantResponse;
import com.store.entity.product.Product;
import com.store.entity.product.ProductVariant;
import com.store.entity.product.ProductVariantStatus;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.ProductRepository;
import com.store.repository.ProductVariantRepository;
import com.store.service.ProductVariantService;
import com.store.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariantsByProductId(Long productId) {
        log.info("Fetching variants for product id {}", productId);
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return productVariantRepository.findByProduct_ProductIdOrderByPriceAsc(productId)
                .stream()
                .map(ProductVariantResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantById(Long variantId) {
        log.info("Fetching variant with id {}", variantId);
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + variantId));
        return ProductVariantResponse.fromEntity(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantBySku(String skuVariant) {
        log.info("Fetching variant with sku {}", skuVariant);
        ProductVariant variant = productVariantRepository.findBySkuVariant(skuVariant)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with SKU: " + skuVariant));
        return ProductVariantResponse.fromEntity(variant);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public ProductVariantResponse createVariant(Long productId, ProductVariantRequest request) {
        log.info("Creating variant for product id {}: {}", productId, request.getVariantName());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (request.getSalePrice() != null && request.getSalePrice().compareTo(request.getPrice()) > 0) {
            throw new IllegalArgumentException("Sale price cannot be greater than regular price.");
        }

        String skuVariant = (request.getSkuVariant() != null && !request.getSkuVariant().isBlank())
                ? request.getSkuVariant().trim().toUpperCase()
                : generateSkuVariant(product, request.getVariantName());

        if (productVariantRepository.existsBySkuVariant(skuVariant)) {
            throw new DuplicateResourceException("Product variant already exists with SKU: " + skuVariant);
        }

        ProductVariantStatus status = request.getStatus() != null
                ? ProductVariantStatus.fromValue(request.getStatus())
                : ProductVariantStatus.ACTIVE;

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .variantName(request.getVariantName().trim())
                .skuVariant(skuVariant)
                .price(request.getPrice())
                .salePrice(request.getSalePrice())
                .costPrice(request.getCostPrice())
                .status(status)
                .build();

        ProductVariant savedVariant = productVariantRepository.save(variant);
        return ProductVariantResponse.fromEntity(savedVariant);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public ProductVariantResponse updateVariant(Long variantId, ProductVariantRequest request) {
        log.info("Updating variant id {}: {}", variantId, request.getVariantName());

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + variantId));

        if (request.getSalePrice() != null && request.getSalePrice().compareTo(request.getPrice()) > 0) {
            throw new IllegalArgumentException("Sale price cannot be greater than regular price.");
        }

        String skuVariant = (request.getSkuVariant() != null && !request.getSkuVariant().isBlank())
                ? request.getSkuVariant().trim().toUpperCase()
                : variant.getSkuVariant();

        if (skuVariant != null && productVariantRepository.existsBySkuVariantAndVariantIdNot(skuVariant, variantId)) {
            throw new DuplicateResourceException("Product variant already exists with SKU: " + skuVariant);
        }

        ProductVariantStatus status = request.getStatus() != null
                ? ProductVariantStatus.fromValue(request.getStatus())
                : variant.getStatus();

        variant.setVariantName(request.getVariantName().trim());
        variant.setSkuVariant(skuVariant);
        variant.setPrice(request.getPrice());
        variant.setSalePrice(request.getSalePrice());
        variant.setCostPrice(request.getCostPrice());
        variant.setStatus(status);

        ProductVariant updatedVariant = productVariantRepository.save(variant);
        return ProductVariantResponse.fromEntity(updatedVariant);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public void deleteVariant(Long variantId) {
        log.info("Deleting variant with id {}", variantId);
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + variantId));
        productVariantRepository.delete(variant);
    }

    private String generateSkuVariant(Product product, String variantName) {
        String baseSku = (product.getSku() != null && !product.getSku().isBlank())
                ? product.getSku()
                : "PROD-" + product.getProductId();
        String variantSlug = SlugUtils.toSlug(variantName).toUpperCase();
        if (variantSlug.length() > 20) {
            variantSlug = variantSlug.substring(0, 20);
        }
        return baseSku + "-" + variantSlug + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
