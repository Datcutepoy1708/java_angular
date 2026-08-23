package com.store.service.impl;

import com.store.dto.request.ProductImageRequest;
import com.store.dto.response.ProductImageResponse;
import com.store.entity.product.ImageType;
import com.store.entity.product.Product;
import com.store.entity.product.ProductImage;
import com.store.entity.product.ProductVariant;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.ProductImageRepository;
import com.store.repository.ProductRepository;
import com.store.repository.ProductVariantRepository;
import com.store.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> getImagesByProductId(Long productId) {
        log.info("Fetching active images for product id {}", productId);
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return productImageRepository.findByProduct_ProductIdAndDeletedAtIsNullOrderBySortOrderAscImageIdAsc(productId)
                .stream()
                .map(ProductImageResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> getDeletedImagesByProductId(Long productId) {
        log.info("Fetching deleted images for product id {}", productId);
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return productImageRepository.findByProduct_ProductIdAndDeletedAtIsNotNull(productId)
                .stream()
                .map(ProductImageResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> getImagesByVariantId(Long variantId) {
        log.info("Fetching images for variant id {}", variantId);
        if (!productVariantRepository.existsById(variantId)) {
            throw new ResourceNotFoundException("Product variant not found with id: " + variantId);
        }
        return productImageRepository.findByVariant_VariantIdOrderBySortOrderAscImageIdAsc(variantId)
                .stream()
                .map(ProductImageResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public ProductImageResponse addImage(Long productId, ProductImageRequest request) {
        log.info("Adding image for product id {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + request.getVariantId()));

            if (!variant.getProduct().getProductId().equals(productId)) {
                throw new IllegalArgumentException("Variant with id " + request.getVariantId() + " does not belong to product with id " + productId);
            }
        }

        ImageType type = request.getImageType() != null
                ? ImageType.fromValue(request.getImageType())
                : ImageType.SUB;

        if (type == ImageType.MAIN) {
            demoteExistingMainImages(productId, variant != null ? variant.getVariantId() : null);
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .variant(variant)
                .imageUrl(request.getImageUrl().trim())
                .imageType(type)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .altText(request.getAltText())
                .build();

        ProductImage savedImage = productImageRepository.save(image);
        return ProductImageResponse.fromEntity(savedImage);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public ProductImageResponse setMainImage(Long imageId) {
        log.info("Setting image id {} as MAIN", imageId);

        ProductImage target = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found with id: " + imageId));

        Long productId = target.getProduct().getProductId();
        Long variantId = target.getVariant() != null ? target.getVariant().getVariantId() : null;

        demoteExistingMainImages(productId, variantId);

        target.setImageType(ImageType.MAIN);
        ProductImage updated = productImageRepository.save(target);
        return ProductImageResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public ProductImageResponse updateImage(Long imageId, ProductImageRequest request) {
        log.info("Updating image id {}", imageId);

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found with id: " + imageId));

        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            image.setImageUrl(request.getImageUrl().trim());
        }

        if (request.getSortOrder() != null) {
            image.setSortOrder(request.getSortOrder());
        }

        if (request.getAltText() != null) {
            image.setAltText(request.getAltText());
        }

        if (request.getImageType() != null) {
            ImageType newType = ImageType.fromValue(request.getImageType());
            if (newType == ImageType.MAIN && image.getImageType() != ImageType.MAIN) {
                Long productId = image.getProduct().getProductId();
                Long variantId = image.getVariant() != null ? image.getVariant().getVariantId() : null;
                demoteExistingMainImages(productId, variantId);
            }
            image.setImageType(newType);
        }

        ProductImage updated = productImageRepository.save(image);
        return ProductImageResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public void softDeleteImage(Long imageId) {
        log.info("Soft-deleting image id {}", imageId);
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found with id: " + imageId));
        if (image.getDeletedAt() != null) {
            throw new IllegalStateException("Image is already deleted");
        }
        image.setDeletedAt(java.time.LocalDateTime.now());
        productImageRepository.save(image);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public void restoreImage(Long imageId) {
        log.info("Restoring image id {}", imageId);
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found with id: " + imageId));
        if (image.getDeletedAt() == null) {
            throw new IllegalStateException("Image is not deleted");
        }
        image.setDeletedAt(null);
        productImageRepository.save(image);
    }

    /** @deprecated Use softDeleteImage instead */
    @Override
    @Deprecated
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public void deleteImage(Long imageId) {
        softDeleteImage(imageId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
    public void updateSortOrders(Long productId, List<Long> imageIdsInOrder) {
        log.info("Reordering images for product id {}", productId);
        for (int i = 0; i < imageIdsInOrder.size(); i++) {
            Long imgId = imageIdsInOrder.get(i);
            ProductImage img = productImageRepository.findById(imgId).orElse(null);
            if (img != null && img.getProduct().getProductId().equals(productId)) {
                img.setSortOrder(i);
                productImageRepository.save(img);
            }
        }
    }

    private void demoteExistingMainImages(Long productId, Long variantId) {
        if (variantId != null) {
            List<ProductImage> oldMains = productImageRepository.findByVariant_VariantIdAndImageType(variantId, ImageType.MAIN);
            for (ProductImage img : oldMains) {
                img.setImageType(ImageType.SUB);
                productImageRepository.save(img);
            }
        } else {
            List<ProductImage> oldMains = productImageRepository.findByProduct_ProductIdAndVariantIsNullAndImageType(productId, ImageType.MAIN);
            for (ProductImage img : oldMains) {
                img.setImageType(ImageType.SUB);
                productImageRepository.save(img);
            }
        }
    }
}
