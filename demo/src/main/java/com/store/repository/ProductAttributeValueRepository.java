package com.store.repository;

import com.store.entity.product.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, Long> {

    List<ProductAttributeValue> findByProductProductId(Long productId);

    List<ProductAttributeValue> findByProductProductIdIn(List<Long> productIds);

    Optional<ProductAttributeValue> findByProductProductIdAndAttributeAttributeId(Long productId, Integer attributeId);

    @Modifying
    @Query("DELETE FROM ProductAttributeValue pav WHERE pav.product.productId = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("DELETE FROM ProductAttributeValue pav WHERE pav.product.productId = :productId AND pav.attribute.attributeId = :attributeId")
    void deleteByProductIdAndAttributeId(@Param("productId") Long productId, @Param("attributeId") Integer attributeId);
}
