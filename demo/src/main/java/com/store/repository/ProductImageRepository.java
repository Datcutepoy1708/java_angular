package com.store.repository;

import com.store.entity.product.ImageType;
import com.store.entity.product.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProduct_ProductIdOrderBySortOrderAscImageIdAsc(Long productId);

    List<ProductImage> findByVariant_VariantIdOrderBySortOrderAscImageIdAsc(Long variantId);

    List<ProductImage> findByProduct_ProductIdAndVariantIsNullOrderBySortOrderAscImageIdAsc(Long productId);

    List<ProductImage> findByProduct_ProductIdAndImageType(Long productId, ImageType imageType);

    List<ProductImage> findByProduct_ProductIdAndVariantIsNullAndImageType(Long productId, ImageType imageType);

    List<ProductImage> findByVariant_VariantIdAndImageType(Long variantId, ImageType imageType);

    long countByProduct_ProductId(Long productId);
}
