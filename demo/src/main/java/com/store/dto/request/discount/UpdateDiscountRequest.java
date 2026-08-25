package com.store.dto.request.discount;

import com.store.entity.discount.DiscountStatus;
import com.store.entity.discount.DiscountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class UpdateDiscountRequest {

    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    private String description;

    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @Positive(message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;

    @Positive(message = "Mức giảm tối đa phải lớn hơn 0")
    private BigDecimal maxDiscountAmount;

    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    private Integer usageLimit;

    @Builder.Default
    private Integer usageLimitPerUser = 1;

    private Integer applicableCategoryId;

    @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc hiệu lực không được để trống")
    private LocalDateTime endDate;

    @NotNull(message = "Trạng thái không được để trống")
    private DiscountStatus status;
}
