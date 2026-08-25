package com.store.service;

import com.store.dto.statistics.CategoryRevenueShareResponse;
import com.store.dto.statistics.DashboardOverviewResponse;
import com.store.dto.statistics.RevenueChartDataPoint;
import com.store.dto.statistics.TopSellingProductResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StatisticsService {

    DashboardOverviewResponse getDashboardOverview(LocalDate startDate, LocalDate endDate);

    List<RevenueChartDataPoint> getRevenueTrend(String period, LocalDate startDate, LocalDate endDate);

    List<TopSellingProductResponse> getTopSellingProducts(int limit, LocalDate startDate, LocalDate endDate);

    Map<String, Long> getOrderStatusDistribution(LocalDate startDate, LocalDate endDate);

    List<CategoryRevenueShareResponse> getCategoryRevenueShare(LocalDate startDate, LocalDate endDate);

    void refreshStatisticsCache();
}
