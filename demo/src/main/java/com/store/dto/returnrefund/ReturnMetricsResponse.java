package com.store.dto.returnrefund;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnMetricsResponse {

    private long totalRequests;
    private long pendingReviewCount;
    private long awaitingItemCount;
    private long refundedCount;
    private long rejectedCount;
    private BigDecimal totalRefundedAmount;
}
