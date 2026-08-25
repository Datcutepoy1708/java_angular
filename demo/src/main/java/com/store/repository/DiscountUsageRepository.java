package com.store.repository;

import com.store.entity.discount.DiscountUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscountUsageRepository extends JpaRepository<DiscountUsage, Long> {

    long countByDiscountDiscountIdAndUserUserId(Long discountId, Long userId);

    List<DiscountUsage> findByDiscountDiscountIdOrderByUsedAtDesc(Long discountId);

    @Modifying
    @Query("DELETE FROM DiscountUsage du WHERE du.order.orderId = :orderId")
    void deleteByOrderOrderId(@Param("orderId") Long orderId);

    long countByDiscountDiscountId(Long discountId);
}
