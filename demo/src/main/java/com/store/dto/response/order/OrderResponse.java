package com.store.dto.response.order;

import com.store.entity.order.Order;
import com.store.entity.order.OrderStatus;
import com.store.entity.order.PaymentMethod;
import com.store.entity.order.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long orderId;
    private String orderCode;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private Long discountId;
    private String discountCode;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();

    @Builder.Default
    private List<OrderStatusHistoryResponse> statusHistory = new ArrayList<>();

    public static OrderResponse fromEntity(Order order, List<OrderItemResponse> itemResponses, List<OrderStatusHistoryResponse> historyResponses) {
        return fromEntity(order, itemResponses, historyResponses, null);
    }

    public static OrderResponse fromEntity(Order order, List<OrderItemResponse> itemResponses, List<OrderStatusHistoryResponse> historyResponses, String discountCode) {
        if (order == null) return null;
        var user = order.getUser();

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .userId(user != null ? user.getUserId() : null)
                .userEmail(user != null ? user.getEmail() : order.getCustomerEmail())
                .userFullName(user != null ? user.getFullName() : order.getReceiverName())
                .addressId(order.getAddress() != null ? order.getAddress().getAddressId() : null)
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingAddress(order.getShippingAddress())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .discountId(order.getDiscountId())
                .discountCode(discountCode)
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses != null ? itemResponses : new ArrayList<>())
                .statusHistory(historyResponses != null ? historyResponses : new ArrayList<>())
                .build();
    }
}
