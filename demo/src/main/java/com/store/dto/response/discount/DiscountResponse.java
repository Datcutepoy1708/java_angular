package com.store.dto.response.discount;

import com.store.entity.discount.DiscountCode;
import com.store.entity.discount.DiscountStatus;
import com.store.entity.discount.DiscountType;
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
public class DiscountResponse {

    private Long discountId;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private Integer usageLimit;
    private Integer usageLimitPerUser;
    private Integer usedCount;
    private Integer applicableCategoryId;
    private String applicableCategoryName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private DiscountStatus status;
    private LocalDateTime createdAt;
    private Boolean isValidNow;

    public static DiscountResponse fromEntity(DiscountCode entity) {
        if (entity == null) return null;
        LocalDateTime now = LocalDateTime.now();
        boolean validNow = entity.getStatus() == DiscountStatus.ACTIVE &&
                !now.isBefore(entity.getStartDate()) &&
                !now.isAfter(entity.getEndDate()) &&
                (entity.getUsageLimit() == null || entity.getUsedCount() < entity.getUsageLimit());

        return DiscountResponse.builder()
                .discountId(entity.getDiscountId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .maxDiscountAmount(entity.getMaxDiscountAmount())
                .minOrderValue(entity.getMinOrderValue())
                .usageLimit(entity.getUsageLimit())
                .usageLimitPerUser(entity.getUsageLimitPerUser())
                .usedCount(entity.getUsedCount())
                .applicableCategoryId(entity.getApplicableCategory() != null ? entity.getApplicableCategory().getCategoryId() : null)
                .applicableCategoryName(entity.getApplicableCategory() != null ? entity.getApplicableCategory().getName() : null)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .isValidNow(validNow)
                .build();
    }
}
