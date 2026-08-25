package com.store.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderMetricsResponse {

    private long totalOrders;
    private long pendingCount;
    private long confirmedCount;
    private long processingCount;
    private long shippingCount;
    private long completedCount;
    private long cancelledCount;
    private long unpaidCount;
    private long paidCount;
    private BigDecimal totalRevenue;
}
