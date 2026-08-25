package com.store.controller;

import com.store.dto.response.ApiResponse;
import com.store.dto.statistics.CategoryRevenueShareResponse;
import com.store.dto.statistics.DashboardOverviewResponse;
import com.store.dto.statistics.RevenueChartDataPoint;
import com.store.dto.statistics.TopSellingProductResponse;
import com.store.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
@Tag(name = "Admin Statistics", description = "Dashboard & Analytics Management APIs")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    @Operation(summary = "Get Dashboard KPI Overview (Revenue, Orders, AOV, Customers, Low stock)")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        DashboardOverviewResponse response = statisticsService.getDashboardOverview(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy số liệu tổng quan thành công", response));
    }

    @GetMapping("/revenue-trend")
    @Operation(summary = "Get time-series revenue and order volume trend (day / month)")
    public ResponseEntity<ApiResponse<List<RevenueChartDataPoint>>> getRevenueTrend(
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<RevenueChartDataPoint> response = statisticsService.getRevenueTrend(period, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy biểu đồ doanh thu thành công", response));
    }

    @GetMapping("/top-selling")
    @Operation(summary = "Get Top selling products by volume and revenue")
    public ResponseEntity<ApiResponse<List<TopSellingProductResponse>>> getTopSelling(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TopSellingProductResponse> response = statisticsService.getTopSellingProducts(limit, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm bán chạy thành công", response));
    }

    @GetMapping("/order-status")
    @Operation(summary = "Get order distribution breakdown by status")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getOrderStatusDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Long> response = statisticsService.getOrderStatusDistribution(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy phân bổ trạng thái đơn hàng thành công", response));
    }

    @GetMapping("/category-share")
    @Operation(summary = "Get category revenue share and percentage")
    public ResponseEntity<ApiResponse<List<CategoryRevenueShareResponse>>> getCategoryShare(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CategoryRevenueShareResponse> response = statisticsService.getCategoryRevenueShare(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy tỷ trọng doanh thu theo danh mục thành công", response));
    }

    @PostMapping("/refresh-cache")
    @Operation(summary = "Evict statistics Redis cache to force re-computation")
    public ResponseEntity<ApiResponse<Void>> refreshCache() {
        statisticsService.refreshStatisticsCache();
        return ResponseEntity.ok(ApiResponse.success("Làm mới bộ nhớ đệm thống kê thành công", null));
    }
}
