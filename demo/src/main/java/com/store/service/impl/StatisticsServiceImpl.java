package com.store.service.impl;

import com.store.dto.statistics.CategoryRevenueShareResponse;
import com.store.dto.statistics.DashboardOverviewResponse;
import com.store.dto.statistics.RevenueChartDataPoint;
import com.store.dto.statistics.TopSellingProductResponse;
import com.store.entity.order.OrderStatus;
import com.store.entity.product.ProductImage;
import com.store.repository.InventoryRepository;
import com.store.repository.OrderItemRepository;
import com.store.repository.OrderRepository;
import com.store.repository.ProductImageRepository;
import com.store.repository.UserRepository;
import com.store.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Cacheable(cacheNames = "statistics", key = "'overview_' + #startDate + '_' + #endDate")
    public DashboardOverviewResponse getDashboardOverview(LocalDate startDate, LocalDate endDate) {
        log.info("Computing Dashboard Overview metrics for range: {} to {}", startDate, endDate);
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        // 1. Total revenue & orders in current period
        BigDecimal totalRevenue = orderRepository.sumRevenueBetween(startDateTime, endDateTime);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        long totalOrders = orderRepository.countOrdersBetween(startDateTime, endDateTime);

        // 2. Average Order Value (AOV)
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (totalOrders > 0 && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP);
        }

        // 3. New registered customers
        long newCustomers = userRepository.countByCreatedAtBetween(startDateTime, endDateTime);

        // 4. Low stock count
        long lowStockCount = inventoryRepository.countLowStockItems() + inventoryRepository.countOutOfStockItems();

        // 5. Calculate growth vs previous period of same length
        long daysBetween = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDateTime prevStartDateTime = start.minusDays(daysBetween).atStartOfDay();
        LocalDateTime prevEndDateTime = start.minusDays(1).atTime(LocalTime.MAX);

        BigDecimal prevRevenue = orderRepository.sumRevenueBetween(prevStartDateTime, prevEndDateTime);
        if (prevRevenue == null) prevRevenue = BigDecimal.ZERO;
        long prevOrders = orderRepository.countOrdersBetween(prevStartDateTime, prevEndDateTime);
        long prevCustomers = userRepository.countByCreatedAtBetween(prevStartDateTime, prevEndDateTime);

        Double revenueGrowthPercent = calculateGrowth(totalRevenue.doubleValue(), prevRevenue.doubleValue());
        Double ordersGrowthPercent = calculateGrowth((double) totalOrders, (double) prevOrders);
        Double customerGrowthPercent = calculateGrowth((double) newCustomers, (double) prevCustomers);

        return DashboardOverviewResponse.builder()
                .totalRevenue(totalRevenue)
                .revenueGrowthPercent(revenueGrowthPercent)
                .totalOrders(totalOrders)
                .ordersGrowthPercent(ordersGrowthPercent)
                .averageOrderValue(averageOrderValue)
                .newCustomers(newCustomers)
                .customerGrowthPercent(customerGrowthPercent)
                .lowStockCount(lowStockCount)
                .build();
    }

    @Override
    @Cacheable(cacheNames = "statistics", key = "'trend_' + #period + '_' + #startDate + '_' + #endDate")
    public List<RevenueChartDataPoint> getRevenueTrend(String period, LocalDate startDate, LocalDate endDate) {
        log.info("Computing Revenue Trend for period: {}, range: {} to {}", period, startDate, endDate);
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        List<RevenueChartDataPoint> result = new ArrayList<>();
        if ("month".equalsIgnoreCase(period)) {
            List<Object[]> rawData = orderRepository.findMonthlyRevenueBetween(startDateTime, endDateTime);
            for (Object[] row : rawData) {
                String label = (row[0] != null) ? row[0].toString() : "";
                BigDecimal rev = (row[1] instanceof BigDecimal) ? (BigDecimal) row[1] : BigDecimal.valueOf(((Number) row[1]).doubleValue());
                Long cnt = (row[2] instanceof Long) ? (Long) row[2] : ((Number) row[2]).longValue();
                result.add(new RevenueChartDataPoint(label, rev, cnt));
            }
        } else {
            // Daily trend
            List<Object[]> rawData = orderRepository.findDailyRevenueBetween(startDateTime, endDateTime);
            Map<String, RevenueChartDataPoint> map = new LinkedHashMap<>();
            for (Object[] row : rawData) {
                String label = (row[0] != null) ? row[0].toString() : "";
                BigDecimal rev = (row[1] instanceof BigDecimal) ? (BigDecimal) row[1] : BigDecimal.valueOf(((Number) row[1]).doubleValue());
                Long cnt = (row[2] instanceof Long) ? (Long) row[2] : ((Number) row[2]).longValue();
                map.put(label, new RevenueChartDataPoint(label, rev, cnt));
            }

            // Fill all dates in range to avoid gaps in chart
            LocalDate current = start;
            while (!current.isAfter(end)) {
                String dateStr = current.toString();
                if (map.containsKey(dateStr)) {
                    result.add(map.get(dateStr));
                } else {
                    result.add(new RevenueChartDataPoint(dateStr, BigDecimal.ZERO, 0L));
                }
                current = current.plusDays(1);
            }
        }

        return result;
    }

    @Override
    @Cacheable(cacheNames = "statistics", key = "'top_selling_' + #limit + '_' + #startDate + '_' + #endDate")
    public List<TopSellingProductResponse> getTopSellingProducts(int limit, LocalDate startDate, LocalDate endDate) {
        log.info("Computing Top Selling Products (limit: {}), range: {} to {}", limit, startDate, endDate);
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        int maxResults = (limit > 0 && limit <= 50) ? limit : 10;
        List<Object[]> rawData = orderItemRepository.findTopSellingProductsBetween(startDateTime, endDateTime, PageRequest.of(0, maxResults));

        List<TopSellingProductResponse> result = new ArrayList<>();
        for (Object[] row : rawData) {
            Long productId = ((Number) row[0]).longValue();
            String name = (row[1] != null) ? row[1].toString() : "";
            String slug = (row[2] != null) ? row[2].toString() : "";
            String categoryName = (row[3] != null) ? row[3].toString() : "";
            Long totalSold = ((Number) row[4]).longValue();
            BigDecimal totalRev = (row[5] instanceof BigDecimal) ? (BigDecimal) row[5] : BigDecimal.valueOf(((Number) row[5]).doubleValue());

            // Fetch primary thumbnail
            String thumbnail = null;
            List<ProductImage> images = productImageRepository.findByProduct_ProductIdAndDeletedAtIsNullOrderBySortOrderAscImageIdAsc(productId);
            if (!images.isEmpty()) {
                thumbnail = images.get(0).getImageUrl();
            }

            result.add(TopSellingProductResponse.builder()
                    .productId(productId)
                    .productName(name)
                    .productSlug(slug)
                    .categoryName(categoryName)
                    .thumbnailImage(thumbnail)
                    .totalQuantitySold(totalSold)
                    .totalRevenueGenerated(totalRev)
                    .build());
        }

        return result;
    }

    @Override
    @Cacheable(cacheNames = "statistics", key = "'order_status_' + #startDate + '_' + #endDate")
    public Map<String, Long> getOrderStatusDistribution(LocalDate startDate, LocalDate endDate) {
        log.info("Computing Order Status Distribution, range: {} to {}", startDate, endDate);
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        Map<String, Long> distribution = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            distribution.put(status.name(), 0L);
        }

        List<Object[]> rawData = orderRepository.countOrdersByStatusBetween(startDateTime, endDateTime);
        for (Object[] row : rawData) {
            if (row[0] != null && row[1] != null) {
                String statusName = row[0].toString();
                Long count = ((Number) row[1]).longValue();
                distribution.put(statusName, count);
            }
        }

        return distribution;
    }

    @Override
    @Cacheable(cacheNames = "statistics", key = "'category_share_' + #startDate + '_' + #endDate")
    public List<CategoryRevenueShareResponse> getCategoryRevenueShare(LocalDate startDate, LocalDate endDate) {
        log.info("Computing Category Revenue Share, range: {} to {}", startDate, endDate);
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        List<Object[]> rawData = orderItemRepository.findCategoryRevenueShareBetween(startDateTime, endDateTime);
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Object[] row : rawData) {
            BigDecimal rev = (row[3] instanceof BigDecimal) ? (BigDecimal) row[3] : BigDecimal.valueOf(((Number) row[3]).doubleValue());
            grandTotal = grandTotal.add(rev);
        }

        List<CategoryRevenueShareResponse> result = new ArrayList<>();
        for (Object[] row : rawData) {
            Long categoryId = ((Number) row[0]).longValue();
            String name = (row[1] != null) ? row[1].toString() : "";
            String slug = (row[2] != null) ? row[2].toString() : "";
            BigDecimal rev = (row[3] instanceof BigDecimal) ? (BigDecimal) row[3] : BigDecimal.valueOf(((Number) row[3]).doubleValue());
            Long orderCount = ((Number) row[4]).longValue();

            double percentage = 0.0;
            if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                percentage = rev.multiply(BigDecimal.valueOf(100))
                        .divide(grandTotal, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            result.add(CategoryRevenueShareResponse.builder()
                    .categoryId(categoryId)
                    .categoryName(name)
                    .categorySlug(slug)
                    .revenue(rev)
                    .orderCount(orderCount)
                    .percentageShare(percentage)
                    .build());
        }

        return result;
    }

    @Override
    @CacheEvict(cacheNames = "statistics", allEntries = true)
    public void refreshStatisticsCache() {
        log.info("Admin triggered manual eviction of 'statistics' Redis cache");
    }

    private Double calculateGrowth(double current, double previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        double growth = ((current - previous) / previous) * 100.0;
        return BigDecimal.valueOf(growth).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
