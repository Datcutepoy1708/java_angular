package com.store.dto.response.discount;

import com.store.entity.discount.DiscountUsage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountUsageResponse {

    private Long id;
    private Long discountId;
    private String discountCode;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Long orderId;
    private String orderCode;
    private BigDecimal orderTotal;
    private LocalDateTime usedAt;

    public static DiscountUsageResponse fromEntity(DiscountUsage entity) {
        if (entity == null) return null;
        return DiscountUsageResponse.builder()
                .id(entity.getId())
                .discountId(entity.getDiscount() != null ? entity.getDiscount().getDiscountId() : null)
                .discountCode(entity.getDiscount() != null ? entity.getDiscount().getCode() : null)
                .userId(entity.getUser() != null ? entity.getUser().getUserId() : null)
                .userFullName(entity.getUser() != null ? entity.getUser().getFullName() : null)
                .userEmail(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .orderId(entity.getOrder() != null ? entity.getOrder().getOrderId() : null)
                .orderCode(entity.getOrder() != null ? entity.getOrder().getOrderCode() : null)
                .orderTotal(entity.getOrder() != null ? entity.getOrder().getTotalAmount() : null)
                .usedAt(entity.getUsedAt())
                .build();
    }
}
