package com.store.repository;

import com.store.entity.order.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderOrderId(Long orderId);

    @Query("SELECT oi.variant.product.productId, oi.variant.product.name, oi.variant.product.slug, " +
           "oi.variant.product.category.name, " +
           "COALESCE(SUM(oi.quantity), 0) as totalSold, " +
           "COALESCE(SUM(oi.subtotal), 0) as totalRevenue " +
           "FROM OrderItem oi " +
           "WHERE oi.order.orderStatus = com.store.entity.order.OrderStatus.COMPLETED " +
           "AND oi.order.createdAt >= :start AND oi.order.createdAt <= :end " +
           "GROUP BY oi.variant.product.productId, oi.variant.product.name, oi.variant.product.slug, oi.variant.product.category.name " +
           "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProductsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT oi.variant.product.category.categoryId, oi.variant.product.category.name, oi.variant.product.category.slug, " +
           "COALESCE(SUM(oi.subtotal), 0) as totalRev, " +
           "COUNT(DISTINCT oi.order.orderId) as orderCnt " +
           "FROM OrderItem oi " +
           "WHERE oi.order.orderStatus = com.store.entity.order.OrderStatus.COMPLETED " +
           "AND oi.order.createdAt >= :start AND oi.order.createdAt <= :end " +
           "GROUP BY oi.variant.product.category.categoryId, oi.variant.product.category.name, oi.variant.product.category.slug " +
           "ORDER BY totalRev DESC")
    List<Object[]> findCategoryRevenueShareBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
