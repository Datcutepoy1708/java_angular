package com.store.dto.response.discount;

import com.store.entity.discount.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountValidationResult {

    private boolean valid;
    private Long discountId;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal subtotal;
    private BigDecimal finalTotal;
    private String description;
    private String message;
}
