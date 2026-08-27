package com.store.service;

import com.store.dto.request.order.CreateOrderRequest;
import com.store.dto.request.order.OrderFilterRequest;
import com.store.dto.request.order.UpdateOrderStatusRequest;
import com.store.dto.request.order.UpdatePaymentStatusRequest;
import com.store.dto.response.order.OrderMetricsResponse;
import com.store.dto.response.order.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    Page<OrderResponse> getUserOrders(Long userId, Pageable pageable);

    OrderResponse getOrderByCode(String orderCode, Long userId);

    OrderResponse trackGuestOrder(String orderCode, String receiverPhone);

    OrderResponse cancelOrderByCustomer(String orderCode, Long userId, String reason);

    Page<OrderResponse> getAdminOrders(OrderFilterRequest filter);

    OrderResponse getAdminOrderById(Long orderId);

    OrderResponse updateOrderStatusByAdmin(Long orderId, UpdateOrderStatusRequest request, Long adminUserId);

    OrderResponse updatePaymentStatusByAdmin(Long orderId, UpdatePaymentStatusRequest request, Long adminUserId);

    OrderMetricsResponse getAdminMetrics();

    int processExpiredPendingOrders();
}
