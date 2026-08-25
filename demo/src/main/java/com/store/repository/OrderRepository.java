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

    boolean existsByOrderCode(String orderCode);
}
