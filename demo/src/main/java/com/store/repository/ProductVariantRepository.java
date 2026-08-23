package com.store.repository;

import com.store.entity.product.ProductVariant;
import com.store.entity.product.ProductVariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // Active only (deleted_at IS NULL)
    List<ProductVariant> findByProduct_ProductIdAndDeletedAtIsNullOrderByPriceAsc(Long productId);

    // Soft-deleted only (for "Đã ẩn" tab in product form)
    List<ProductVariant> findByProduct_ProductIdAndDeletedAtIsNotNull(Long productId);

    List<ProductVariant> findByProduct_ProductIdAndStatusOrderByPriceAsc(Long productId, ProductVariantStatus status);

    Optional<ProductVariant> findBySkuVariant(String skuVariant);

    boolean existsBySkuVariant(String skuVariant);
    boolean existsBySkuVariantAndVariantIdNot(String skuVariant, Long variantId);
    long countByProduct_ProductId(Long productId);
}
