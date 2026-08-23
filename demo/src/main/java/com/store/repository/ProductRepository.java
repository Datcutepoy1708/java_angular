package com.store.repository;

import com.store.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlugAndDeletedAtIsNull(String slug);

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySkuAndDeletedAtIsNull(String sku);

    Optional<Product> findBySku(String sku);

    Page<Product> findByDeletedAtIsNotNull(Pageable pageable);

    boolean existsBySlug(String slug);
    boolean existsBySlugAndProductIdNot(String slug, Long productId);
    boolean existsBySku(String sku);
    boolean existsBySkuAndProductIdNot(String sku, Long productId);
    boolean existsByCategory_CategoryId(Integer categoryId);
    boolean existsByBrand_BrandId(Integer brandId);
}
