package com.store.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueChartDataPoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private String dateLabel;     // e.g. "2026-08-20" or "T8/2026"
    private BigDecimal revenue;   // Total revenue for this day/month
    private Long orderCount;      // Number of orders for this day/month
}
