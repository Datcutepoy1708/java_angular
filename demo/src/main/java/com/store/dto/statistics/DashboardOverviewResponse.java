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
public class DashboardOverviewResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    // Total revenue in selected period
    private BigDecimal totalRevenue;
    private Double revenueGrowthPercent; // e.g. +12.5% vs previous period

    // Total orders in selected period
    private Long totalOrders;
    private Double ordersGrowthPercent;

    // Average Order Value (AOV)
    private BigDecimal averageOrderValue;

    // New registered customers in period
    private Long newCustomers;
    private Double customerGrowthPercent;

    // Current low stock variants alert count
    private Long lowStockCount;
}
