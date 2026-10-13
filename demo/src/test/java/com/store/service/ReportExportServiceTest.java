package com.store.service;

import com.store.dto.request.inventory.InventoryFilterRequest;
import com.store.dto.request.order.OrderFilterRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.inventory.InventoryResponse;
import com.store.dto.response.order.OrderResponse;
import com.store.dto.statistics.DashboardOverviewResponse;
import com.store.dto.statistics.RevenueChartDataPoint;
import com.store.dto.statistics.TopSellingProductResponse;
import com.store.entity.order.OrderStatus;
import com.store.entity.order.PaymentMethod;
import com.store.entity.order.PaymentStatus;
import com.store.service.impl.ReportExportServiceImpl;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {

    @Mock
    private StatisticsService statisticsService;

    @Mock
    private OrderService orderService;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private ReportExportServiceImpl reportExportService;

    @Test
    @DisplayName("exportRevenueReport should generate valid Excel workbook with two sheets")
    void exportRevenueReport_ShouldGenerateValidWorkbook() throws IOException {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();

        DashboardOverviewResponse overview = DashboardOverviewResponse.builder()
                .totalRevenue(new BigDecimal("15000000"))
                .totalOrders(10L)
                .averageOrderValue(new BigDecimal("1500000"))
                .newCustomers(5L)
                .lowStockCount(2L)
                .build();

        RevenueChartDataPoint dp = RevenueChartDataPoint.builder()
                .dateLabel("2026-10-10")
                .revenue(new BigDecimal("5000000"))
                .orderCount(3L)
                .build();

        TopSellingProductResponse top = TopSellingProductResponse.builder()
                .productId(101L)
                .productName("CPU Intel Core i5 13400")
                .categoryName("CPU")
                .totalQuantitySold(8L)
                .totalRevenueGenerated(new BigDecimal("40000000"))
                .build();

        when(statisticsService.getDashboardOverview(start, end)).thenReturn(overview);
        when(statisticsService.getRevenueTrend("day", start, end)).thenReturn(List.of(dp));
        when(statisticsService.getTopSellingProducts(20, start, end)).thenReturn(List.of(top));

        byte[] result = reportExportService.exportRevenueReport(start, end, "day");

        assertThat(result).isNotNull().isNotEmpty();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            Sheet sheet1 = wb.getSheet("Tổng quan doanh thu");
            assertThat(sheet1).isNotNull();
            assertThat(sheet1.getRow(0).getCell(0).getStringCellValue()).contains("BÁO CÁO DOANH THU");

            Sheet sheet2 = wb.getSheet("Top sản phẩm bán chạy");
            assertThat(sheet2).isNotNull();
            assertThat(sheet2.getRow(0).getCell(0).getStringCellValue()).contains("TOP SẢN PHẨM BÁN CHẠY");
        }
    }

    @Test
    @DisplayName("exportOrdersReport should generate valid Excel workbook with orders sheet")
    void exportOrdersReport_ShouldGenerateValidWorkbook() throws IOException {
        OrderResponse order = OrderResponse.builder()
                .orderId(1L)
                .orderCode("ORD-20261013-001")
                .receiverName("Nguyễn Văn A")
                .receiverPhone("0912345678")
                .shippingAddress("Hà Nội")
                .subtotal(new BigDecimal("10000000"))
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(new BigDecimal("30000"))
                .totalAmount(new BigDecimal("10030000"))
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.UNPAID)
                .orderStatus(OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderService.getAdminOrders(any(OrderFilterRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        byte[] result = reportExportService.exportOrdersReport(new OrderFilterRequest());

        assertThat(result).isNotNull().isNotEmpty();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheet("Danh sách đơn hàng");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).contains("DANH SÁCH ĐƠN HÀNG");
            assertThat(sheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("ORD-20261013-001");
        }
    }

    @Test
    @DisplayName("exportInventoryReport should generate valid Excel workbook with inventory sheet")
    void exportInventoryReport_ShouldGenerateValidWorkbook() throws IOException {
        InventoryResponse inv = InventoryResponse.builder()
                .inventoryId(1L)
                .skuVariant("CPU-INTEL-13400")
                .productName("CPU Intel Core i5 13400")
                .variantName("Box Chính Hãng")
                .warehouseName("Kho Tổng Hà Nội")
                .price(new BigDecimal("5000000"))
                .salePrice(new BigDecimal("4800000"))
                .quantity(50)
                .reservedQty(5)
                .availableQty(45)
                .stockStatus("IN_STOCK")
                .updatedAt(LocalDateTime.now())
                .build();

        PageResponse<InventoryResponse> pageResponse = PageResponse.<InventoryResponse>builder()
                .content(List.of(inv))
                .totalElements(1)
                .totalPages(1)
                .pageNumber(0)
                .pageSize(5000)
                .build();

        when(inventoryService.getInventoryPage(any(InventoryFilterRequest.class))).thenReturn(pageResponse);

        byte[] result = reportExportService.exportInventoryReport(null, null, "ALL");

        assertThat(result).isNotNull().isNotEmpty();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheet("Báo cáo tồn kho");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).contains("BÁO CÁO TỒN KHO");
            assertThat(sheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("CPU-INTEL-13400");
        }
    }
}
