package com.store.repository;

import com.store.entity.product.ProductVariant;
import com.store.entity.product.ProductVariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProduct_ProductIdOrderByPriceAsc(Long productId);

    List<ProductVariant> findByProduct_ProductIdAndStatusOrderByPriceAsc(Long productId, ProductVariantStatus status);

    Optional<ProductVariant> findBySkuVariant(String skuVariant);

    boolean existsBySkuVariant(String skuVariant);

    boolean existsBySkuVariantAndVariantIdNot(String skuVariant, Long variantId);

    long countByProduct_ProductId(Long productId);
}
