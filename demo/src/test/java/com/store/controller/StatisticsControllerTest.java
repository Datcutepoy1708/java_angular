package com.store.controller;

import com.store.dto.statistics.DashboardOverviewResponse;
import com.store.dto.statistics.RevenueChartDataPoint;
import com.store.dto.statistics.TopSellingProductResponse;
import com.store.service.StatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private StatisticsController statisticsController;

    @Test
    @DisplayName("GET /api/v1/admin/statistics/overview returns overview response")
    void testGetOverview() {
        DashboardOverviewResponse mockOverview = DashboardOverviewResponse.builder()
                .totalRevenue(BigDecimal.valueOf(50000000))
                .totalOrders(25L)
                .build();

        when(statisticsService.getDashboardOverview(any(), any())).thenReturn(mockOverview);

        var response = statisticsController.getOverview(LocalDate.now().minusDays(7), LocalDate.now());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(50000000));
    }

    @Test
    @DisplayName("GET /api/v1/admin/statistics/revenue-trend returns trend list")
    void testGetRevenueTrend() {
        List<RevenueChartDataPoint> mockTrend = List.of(
                new RevenueChartDataPoint("2026-08-20", BigDecimal.valueOf(10000000), 2L)
        );
        when(statisticsService.getRevenueTrend(eq("day"), any(), any())).thenReturn(mockTrend);

        var response = statisticsController.getRevenueTrend("day", LocalDate.now().minusDays(7), LocalDate.now());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/admin/statistics/refresh-cache triggers cache eviction")
    void testRefreshCache() {
        var response = statisticsController.refreshCache();

        verify(statisticsService).refreshStatisticsCache();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
