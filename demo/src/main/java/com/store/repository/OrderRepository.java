package com.store.repository;

import com.store.entity.order.Order;
import com.store.entity.order.OrderStatus;
import com.store.entity.order.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByOrderCodeAndUserUserId(String orderCode, Long userId);

    Page<Order> findByUserUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Order> findByOrderStatusAndCreatedAtBefore(OrderStatus orderStatus, LocalDateTime cutoffTime);

    long countByOrderStatus(OrderStatus orderStatus);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus = com.store.entity.order.OrderStatus.COMPLETED")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus = com.store.entity.order.OrderStatus.COMPLETED AND o.createdAt >= :start AND o.createdAt <= :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus != com.store.entity.order.OrderStatus.CANCELLED AND o.createdAt >= :start AND o.createdAt <= :end")
    long countOrdersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('DATE', o.createdAt) as dateStr, COALESCE(SUM(o.totalAmount), 0) as rev, COUNT(o) as cnt " +
           "FROM Order o WHERE o.orderStatus = com.store.entity.order.OrderStatus.COMPLETED " +
           "AND o.createdAt >= :start AND o.createdAt <= :end " +
           "GROUP BY FUNCTION('DATE', o.createdAt) ORDER BY FUNCTION('DATE', o.createdAt) ASC")
    List<Object[]> findDailyRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('DATE_FORMAT', o.createdAt, '%Y-%m') as monthStr, COALESCE(SUM(o.totalAmount), 0) as rev, COUNT(o) as cnt " +
           "FROM Order o WHERE o.orderStatus = com.store.entity.order.OrderStatus.COMPLETED " +
           "AND o.createdAt >= :start AND o.createdAt <= :end " +
           "GROUP BY FUNCTION('DATE_FORMAT', o.createdAt, '%Y-%m') ORDER BY FUNCTION('DATE_FORMAT', o.createdAt, '%Y-%m') ASC")
    List<Object[]> findMonthlyRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT o.orderStatus, COUNT(o) FROM Order o WHERE o.createdAt >= :start AND o.createdAt <= :end GROUP BY o.orderStatus")
    List<Object[]> countOrdersByStatusBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByUserUserId(Long userId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user.userId = :userId AND o.orderStatus = com.store.entity.order.OrderStatus.COMPLETED")
    BigDecimal sumTotalSpendByUserId(@Param("userId") Long userId);

    boolean existsByOrderCode(String orderCode);
}
