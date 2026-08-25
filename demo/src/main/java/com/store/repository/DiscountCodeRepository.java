package com.store.repository;

import com.store.entity.discount.DiscountCode;
import com.store.entity.discount.DiscountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long>, JpaSpecificationExecutor<DiscountCode> {

    Optional<DiscountCode> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndDiscountIdNot(String code, Long discountId);

    @Modifying
    @Query("UPDATE DiscountCode d SET d.usedCount = d.usedCount + 1 " +
           "WHERE d.discountId = :id " +
           "AND d.status = 'active' " +
           "AND :now BETWEEN d.startDate AND d.endDate " +
           "AND (d.usageLimit IS NULL OR d.usedCount < d.usageLimit)")
    int incrementUsedCountAtomic(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE DiscountCode d SET d.usedCount = GREATEST(0, d.usedCount - 1) " +
           "WHERE d.discountId = :id")
    int decrementUsedCountAtomic(@Param("id") Long id);

    @Query("SELECT d FROM DiscountCode d " +
           "WHERE d.status = 'active' " +
           "AND :now BETWEEN d.startDate AND d.endDate " +
           "AND (d.usageLimit IS NULL OR d.usedCount < d.usageLimit) " +
           "ORDER BY d.createdAt DESC")
    List<DiscountCode> findActivePublicDiscounts(@Param("now") LocalDateTime now);

    long countByStatus(DiscountStatus status);

    @Query("SELECT COALESCE(SUM(d.usedCount), 0) FROM DiscountCode d")
    long sumAllUsedCount();

    @Query("SELECT COUNT(d) FROM DiscountCode d WHERE d.endDate < :now OR d.status = 'expired'")
    long countExpiredDiscounts(@Param("now") LocalDateTime now);
}
