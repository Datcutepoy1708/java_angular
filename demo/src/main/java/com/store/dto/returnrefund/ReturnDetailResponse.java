package com.store.dto.returnrefund;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnDetailResponse {

    private Long returnId;
    private String returnCode;
    private Long orderId;
    private String orderTrackingNumber;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String status;
    private String returnReason;
    private String customerNote;
    private String adminNote;
    private BigDecimal refundAmount;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountName;
    private String refundTransactionCode;
    private Integer restockWarehouseId;
    private String restockWarehouseName;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReturnItemDetail> items;
    private List<String> imageUrls;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnItemDetail {
        private Long id;
        private Long orderItemId;
        private Long variantId;
        private String productName;
        private String variantName;
        private String skuVariant;
        private String imageUrl;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String itemCondition;
    }
}
