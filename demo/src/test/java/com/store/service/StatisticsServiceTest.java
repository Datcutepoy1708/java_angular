package com.store.service;

import com.store.dto.statistics.CategoryRevenueShareResponse;
import com.store.dto.statistics.DashboardOverviewResponse;
import com.store.dto.statistics.RevenueChartDataPoint;
import com.store.dto.statistics.TopSellingProductResponse;
import com.store.entity.order.OrderStatus;
import com.store.repository.InventoryRepository;
import com.store.repository.OrderItemRepository;
import com.store.repository.OrderRepository;
import com.store.repository.ProductImageRepository;
import com.store.repository.UserRepository;
import com.store.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    private LocalDate start;
    private LocalDate end;

    @BeforeEach
    void setUp() {
        start = LocalDate.of(2026, 8, 1);
        end = LocalDate.of(2026, 8, 25);
    }

    @Test
    @DisplayName("getDashboardOverview should compute correct metrics and growth rates")
    void testGetDashboardOverview_Success() {
        when(orderRepository.sumRevenueBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(100000000)) // current
                .thenReturn(BigDecimal.valueOf(80000000));  // previous

        when(orderRepository.countOrdersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(50L)  // current
                .thenReturn(40L);  // previous

        when(userRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(20L)  // current
                .thenReturn(10L);  // previous

        when(inventoryRepository.countLowStockItems()).thenReturn(4L);
        when(inventoryRepository.countOutOfStockItems()).thenReturn(1L);

        DashboardOverviewResponse overview = statisticsService.getDashboardOverview(start, end);

        assertThat(overview).isNotNull();
        assertThat(overview.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(100000000));
        assertThat(overview.getTotalOrders()).isEqualTo(50L);
        assertThat(overview.getAverageOrderValue()).isEqualByComparingTo(BigDecimal.valueOf(2000000));
        assertThat(overview.getNewCustomers()).isEqualTo(20L);
        assertThat(overview.getLowStockCount()).isEqualTo(5L);
        assertThat(overview.getRevenueGrowthPercent()).isEqualTo(25.0);
        assertThat(overview.getOrdersGrowthPercent()).isEqualTo(25.0);
        assertThat(overview.getCustomerGrowthPercent()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getRevenueTrend should return daily points for all days in range")
    void testGetRevenueTrend_Daily_Success() {
        LocalDate s = LocalDate.of(2026, 8, 20);
        LocalDate e = LocalDate.of(2026, 8, 22);

        List<Object[]> rawData = java.util.Collections.singletonList(
                new Object[]{"2026-08-21", BigDecimal.valueOf(15000000), 3L}
        );
        when(orderRepository.findDailyRevenueBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rawData);

        List<RevenueChartDataPoint> trend = statisticsService.getRevenueTrend("day", s, e);

        assertThat(trend).hasSize(3);
        assertThat(trend.get(0).getDateLabel()).isEqualTo("2026-08-20");
        assertThat(trend.get(0).getRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(trend.get(1).getDateLabel()).isEqualTo("2026-08-21");
        assertThat(trend.get(1).getRevenue()).isEqualByComparingTo(BigDecimal.valueOf(15000000));
        assertThat(trend.get(1).getOrderCount()).isEqualTo(3L);
        assertThat(trend.get(2).getDateLabel()).isEqualTo("2026-08-22");
    }

    @Test
    @DisplayName("getTopSellingProducts should return products mapped properly")
    void testGetTopSellingProducts_Success() {
        List<Object[]> rawData = java.util.Collections.singletonList(
                new Object[]{1L, "RTX 4070 Super", "rtx-4070-super", "VGA", 25L, BigDecimal.valueOf(450000000)}
        );
        when(orderItemRepository.findTopSellingProductsBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(PageRequest.of(0, 10))))
                .thenReturn(rawData);
        when(productImageRepository.findByProduct_ProductIdAndDeletedAtIsNullOrderBySortOrderAscImageIdAsc(1L))
                .thenReturn(Collections.emptyList());

        List<TopSellingProductResponse> topSelling = statisticsService.getTopSellingProducts(10, start, end);

        assertThat(topSelling).hasSize(1);
        assertThat(topSelling.get(0).getProductId()).isEqualTo(1L);
        assertThat(topSelling.get(0).getProductName()).isEqualTo("RTX 4070 Super");
        assertThat(topSelling.get(0).getTotalQuantitySold()).isEqualTo(25L);
        assertThat(topSelling.get(0).getTotalRevenueGenerated()).isEqualByComparingTo(BigDecimal.valueOf(450000000));
    }

    @Test
    @DisplayName("getOrderStatusDistribution should initialize all enum statuses and fill counts")
    void testGetOrderStatusDistribution_Success() {
        List<Object[]> rawData = java.util.Arrays.asList(
                new Object[]{"COMPLETED", 35L},
                new Object[]{"PENDING", 5L}
        );
        when(orderRepository.countOrdersByStatusBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rawData);

        Map<String, Long> distribution = statisticsService.getOrderStatusDistribution(start, end);

        assertThat(distribution).isNotNull();
        assertThat(distribution.get("COMPLETED")).isEqualTo(35L);
        assertThat(distribution.get("PENDING")).isEqualTo(5L);
        assertThat(distribution.get("CANCELLED")).isEqualTo(0L);
    }

    @Test
    @DisplayName("getCategoryRevenueShare should compute percentage share accurately")
    void testGetCategoryRevenueShare_Success() {
        List<Object[]> rawData = java.util.Arrays.asList(
                new Object[]{10L, "VGA - Card màn hình", "vga", BigDecimal.valueOf(60000000), 10L},
                new Object[]{20L, "CPU - Vi xử lý", "cpu", BigDecimal.valueOf(40000000), 8L}
        );
        when(orderItemRepository.findCategoryRevenueShareBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rawData);

        List<CategoryRevenueShareResponse> shares = statisticsService.getCategoryRevenueShare(start, end);

        assertThat(shares).hasSize(2);
        assertThat(shares.get(0).getCategoryName()).isEqualTo("VGA - Card màn hình");
        assertThat(shares.get(0).getPercentageShare()).isEqualTo(60.0);
        assertThat(shares.get(1).getCategoryName()).isEqualTo("CPU - Vi xử lý");
        assertThat(shares.get(1).getPercentageShare()).isEqualTo(40.0);
    }
}
