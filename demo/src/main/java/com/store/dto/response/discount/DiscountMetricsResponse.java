package com.store.dto.response.discount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountMetricsResponse {

    private long totalDiscounts;
    private long activeDiscounts;
    private long totalUsedCount;
    private long expiredDiscounts;
}
