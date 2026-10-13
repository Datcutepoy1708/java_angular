package com.store.service.impl;

import com.store.dto.request.inventory.InventoryFilterRequest;
import com.store.dto.request.order.OrderFilterRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.inventory.InventoryResponse;
import com.store.dto.response.order.OrderResponse;
import com.store.dto.statistics.DashboardOverviewResponse;
import com.store.dto.statistics.RevenueChartDataPoint;
import com.store.dto.statistics.TopSellingProductResponse;
import com.store.service.InventoryService;
import com.store.service.OrderService;
import com.store.service.ReportExportService;
import com.store.service.StatisticsService;
import com.store.util.ExcelExportHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportExportServiceImpl implements ReportExportService {

    private final StatisticsService statisticsService;
    private final OrderService orderService;
    private final InventoryService inventoryService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public byte[] exportRevenueReport(LocalDate startDate, LocalDate endDate, String period) {
        log.info("Generating Excel Revenue Report from {} to {} (period={})", startDate, endDate, period);
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();
        String reportPeriod = (period != null && !period.isBlank()) ? period : "day";

        DashboardOverviewResponse overview = statisticsService.getDashboardOverview(start, end);
        List<RevenueChartDataPoint> trend = statisticsService.getRevenueTrend(reportPeriod, start, end);
        List<TopSellingProductResponse> topSelling = statisticsService.getTopSellingProducts(20, start, end);

        try (Workbook workbook = ExcelExportHelper.createWorkbook()) {
            CellStyle titleStyle = ExcelExportHelper.createTitleStyle(workbook);
            CellStyle subtitleStyle = ExcelExportHelper.createSubtitleStyle(workbook);
            CellStyle headerStyle = ExcelExportHelper.createHeaderStyle(workbook);
            CellStyle textStyle = ExcelExportHelper.createDataStyle(workbook, HorizontalAlignment.LEFT);
            CellStyle centerStyle = ExcelExportHelper.createDataStyle(workbook, HorizontalAlignment.CENTER);
            CellStyle currencyStyle = ExcelExportHelper.createCurrencyStyle(workbook);
            CellStyle integerStyle = ExcelExportHelper.createIntegerStyle(workbook);
            CellStyle totalStyle = ExcelExportHelper.createTotalStyle(workbook);

            // ==========================================
            // SHEET 1: TỔNG QUAN DOANH THU & XU HƯỚNG
            // ==========================================
            Sheet sheetOverview = workbook.createSheet("Tổng quan doanh thu");
            int rowIndex = 0;

            // Title
            Row titleRow = sheetOverview.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO DOANH THU & HIỆU QUẢ KINH DOANH");
            titleCell.setCellStyle(titleStyle);

            Row subRow = sheetOverview.createRow(rowIndex++);
            Cell subCell = subRow.createCell(0);
            subCell.setCellValue("Khoảng thời gian: Từ " + start.format(DATE_FORMATTER) + " đến " + end.format(DATE_FORMATTER) + " | Ngày xuất: " + LocalDateTime.now().format(DATE_TIME_FORMATTER));
            subCell.setCellStyle(subtitleStyle);
            rowIndex++; // Empty space

            // KPI Summary table
            Row kpiHeaderRow = sheetOverview.createRow(rowIndex++);
            createCell(kpiHeaderRow, 0, "Chỉ số kinh doanh chính (KPI)", headerStyle);
            createCell(kpiHeaderRow, 1, "Giá trị", headerStyle);

            addKpiRow(sheetOverview, rowIndex++, "Tổng doanh thu", overview != null && overview.getTotalRevenue() != null ? overview.getTotalRevenue() : BigDecimal.ZERO, currencyStyle, textStyle);
            addKpiRow(sheetOverview, rowIndex++, "Tổng đơn hàng thành công", overview != null ? overview.getTotalOrders() : 0L, integerStyle, textStyle);
            addKpiRow(sheetOverview, rowIndex++, "Giá trị đơn trung bình (AOV)", overview != null && overview.getAverageOrderValue() != null ? overview.getAverageOrderValue() : BigDecimal.ZERO, currencyStyle, textStyle);
            addKpiRow(sheetOverview, rowIndex++, "Khách hàng mới", overview != null ? overview.getNewCustomers() : 0L, integerStyle, textStyle);
            addKpiRow(sheetOverview, rowIndex++, "Sản phẩm cảnh báo tồn kho thấp", overview != null ? overview.getLowStockCount() : 0L, integerStyle, textStyle);

            rowIndex += 2; // Empty space

            // Trend table
            Row trendTitleRow = sheetOverview.createRow(rowIndex++);
            Cell trendTitleCell = trendTitleRow.createCell(0);
            trendTitleCell.setCellValue("CHI TIẾT DOANH THU THEO THỜI GIAN");
            trendTitleCell.setCellStyle(headerStyle);

            Row trendHeaderRow = sheetOverview.createRow(rowIndex++);
            createCell(trendHeaderRow, 0, "STT", headerStyle);
            createCell(trendHeaderRow, 1, "Mốc thời gian", headerStyle);
            createCell(trendHeaderRow, 2, "Số đơn hàng", headerStyle);
            createCell(trendHeaderRow, 3, "Doanh thu (VNĐ)", headerStyle);

            int seq = 1;
            BigDecimal sumRevenue = BigDecimal.ZERO;
            long sumOrders = 0;
            if (trend != null) {
                for (RevenueChartDataPoint dp : trend) {
                    Row row = sheetOverview.createRow(rowIndex++);
                    createCell(row, 0, seq++, centerStyle);
                    createCell(row, 1, dp.getDateLabel() != null ? dp.getDateLabel() : "", centerStyle);
                    createCell(row, 2, dp.getOrderCount() != null ? dp.getOrderCount() : 0L, integerStyle);
                    createCell(row, 3, dp.getRevenue() != null ? dp.getRevenue() : BigDecimal.ZERO, currencyStyle);

                    if (dp.getRevenue() != null) sumRevenue = sumRevenue.add(dp.getRevenue());
                    if (dp.getOrderCount() != null) sumOrders += dp.getOrderCount();
                }
            }

            // Trend Total Row
            Row totalRow = sheetOverview.createRow(rowIndex++);
            createCell(totalRow, 0, "", totalStyle);
            createCell(totalRow, 1, "TỔNG CỘNG", totalStyle);
            createCell(totalRow, 2, sumOrders, totalStyle);
            createCell(totalRow, 3, sumRevenue, totalStyle);

            ExcelExportHelper.autoSizeColumns(sheetOverview, 4);

            // ==========================================
            // SHEET 2: TOP SẢN PHẨM BÁN CHẠY
            // ==========================================
            Sheet sheetTop = workbook.createSheet("Top sản phẩm bán chạy");
            int topRowIdx = 0;

            Row topTitleRow = sheetTop.createRow(topRowIdx++);
            Cell topTitle = topTitleRow.createCell(0);
            topTitle.setCellValue("DANH SÁCH TOP SẢN PHẨM BÁN CHẠY NHẤT");
            topTitle.setCellStyle(titleStyle);

            Row topSub = sheetTop.createRow(topRowIdx++);
            Cell topSubCell = topSub.createCell(0);
            topSubCell.setCellValue("Kỳ báo cáo: Từ " + start.format(DATE_FORMATTER) + " đến " + end.format(DATE_FORMATTER));
            topSubCell.setCellStyle(subtitleStyle);
            topRowIdx++;

            Row topHeader = sheetTop.createRow(topRowIdx++);
            createCell(topHeader, 0, "Top", headerStyle);
            createCell(topHeader, 1, "Mã SP", headerStyle);
            createCell(topHeader, 2, "Tên sản phẩm", headerStyle);
            createCell(topHeader, 3, "Danh mục", headerStyle);
            createCell(topHeader, 4, "Số lượng đã bán", headerStyle);
            createCell(topHeader, 5, "Tổng doanh số (VNĐ)", headerStyle);

            int topSeq = 1;
            BigDecimal topTotalRev = BigDecimal.ZERO;
            long topTotalQty = 0;
            if (topSelling != null) {
                for (TopSellingProductResponse item : topSelling) {
                    Row row = sheetTop.createRow(topRowIdx++);
                    createCell(row, 0, topSeq++, centerStyle);
                    createCell(row, 1, item.getProductId() != null ? item.getProductId().toString() : "", centerStyle);
                    createCell(row, 2, item.getProductName() != null ? item.getProductName() : "", textStyle);
                    createCell(row, 3, item.getCategoryName() != null ? item.getCategoryName() : "", textStyle);
                    createCell(row, 4, item.getTotalQuantitySold() != null ? item.getTotalQuantitySold() : 0L, integerStyle);
                    createCell(row, 5, item.getTotalRevenueGenerated() != null ? item.getTotalRevenueGenerated() : BigDecimal.ZERO, currencyStyle);

                    if (item.getTotalQuantitySold() != null) topTotalQty += item.getTotalQuantitySold();
                    if (item.getTotalRevenueGenerated() != null) topTotalRev = topTotalRev.add(item.getTotalRevenueGenerated());
                }
            }

            // Top Total Row
            Row topSummaryRow = sheetTop.createRow(topRowIdx++);
            createCell(topSummaryRow, 0, "", totalStyle);
            createCell(topSummaryRow, 1, "", totalStyle);
            createCell(topSummaryRow, 2, "TỔNG CỘNG", totalStyle);
            createCell(topSummaryRow, 3, "", totalStyle);
            createCell(topSummaryRow, 4, topTotalQty, totalStyle);
            createCell(topSummaryRow, 5, topTotalRev, totalStyle);

            ExcelExportHelper.autoSizeColumns(sheetTop, 6);

            return ExcelExportHelper.writeToByteArray(workbook);
        } catch (IOException e) {
            log.error("Failed to generate Revenue Excel file", e);
            throw new RuntimeException("Không thể tạo file báo cáo Excel doanh thu: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportOrdersReport(OrderFilterRequest filter) {
        log.info("Generating Excel Orders Report with filter: {}", filter);
        OrderFilterRequest exportFilter = filter != null ? filter : new OrderFilterRequest();
        exportFilter.setPage(0);
        exportFilter.setSize(5000); // Export up to 5000 orders in batch

        Page<OrderResponse> ordersPage = orderService.getAdminOrders(exportFilter);
        List<OrderResponse> orders = ordersPage.getContent();

        try (Workbook workbook = ExcelExportHelper.createWorkbook()) {
            CellStyle titleStyle = ExcelExportHelper.createTitleStyle(workbook);
            CellStyle subtitleStyle = ExcelExportHelper.createSubtitleStyle(workbook);
            CellStyle headerStyle = ExcelExportHelper.createHeaderStyle(workbook);
            CellStyle textStyle = ExcelExportHelper.createDataStyle(workbook, HorizontalAlignment.LEFT);
            CellStyle centerStyle = ExcelExportHelper.createDataStyle(workbook, HorizontalAlignment.CENTER);
            CellStyle currencyStyle = ExcelExportHelper.createCurrencyStyle(workbook);
            CellStyle totalStyle = ExcelExportHelper.createTotalStyle(workbook);

            Sheet sheet = workbook.createSheet("Danh sách đơn hàng");
            int rowIndex = 0;

            // Title
            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO DANH SÁCH ĐƠN HÀNG");
            titleCell.setCellStyle(titleStyle);

            Row subRow = sheet.createRow(rowIndex++);
            Cell subCell = subRow.createCell(0);
            subCell.setCellValue("Tổng số đơn: " + orders.size() + " | Thời gian xuất: " + LocalDateTime.now().format(DATE_TIME_FORMATTER));
            subCell.setCellStyle(subtitleStyle);
            rowIndex++;

            // Table headers
            String[] headers = {
                    "STT", "Mã đơn hàng", "Ngày tạo", "Tên người nhận", "Số điện thoại",
                    "Địa chỉ giao hàng", "Phương thức thanh toán", "Trạng thái thanh toán",
                    "Trạng thái đơn hàng", "Tạm tính (VNĐ)", "Giảm giá (VNĐ)", "Phí ship (VNĐ)",
                    "Tổng tiền (VNĐ)", "Ghi chú"
            };
            Row headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            int seq = 1;
            BigDecimal totalRevenueSum = BigDecimal.ZERO;
            BigDecimal totalDiscountSum = BigDecimal.ZERO;

            for (OrderResponse o : orders) {
                Row row = sheet.createRow(rowIndex++);
                createCell(row, 0, seq++, centerStyle);
                createCell(row, 1, o.getOrderCode() != null ? o.getOrderCode() : "", centerStyle);
                createCell(row, 2, o.getCreatedAt() != null ? o.getCreatedAt().format(DATE_TIME_FORMATTER) : "", centerStyle);
                createCell(row, 3, o.getReceiverName() != null ? o.getReceiverName() : "", textStyle);
                createCell(row, 4, o.getReceiverPhone() != null ? o.getReceiverPhone() : "", centerStyle);
                createCell(row, 5, o.getShippingAddress() != null ? o.getShippingAddress() : "", textStyle);
                createCell(row, 6, o.getPaymentMethod() != null ? o.getPaymentMethod().name() : "", centerStyle);
                createCell(row, 7, o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "", centerStyle);
                createCell(row, 8, o.getOrderStatus() != null ? o.getOrderStatus().name() : "", centerStyle);
                createCell(row, 9, o.getSubtotal() != null ? o.getSubtotal() : BigDecimal.ZERO, currencyStyle);
                createCell(row, 10, o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO, currencyStyle);
                createCell(row, 11, o.getShippingFee() != null ? o.getShippingFee() : BigDecimal.ZERO, currencyStyle);
                createCell(row, 12, o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO, currencyStyle);
                createCell(row, 13, o.getNote() != null ? o.getNote() : "", textStyle);

                if (o.getTotalAmount() != null) totalRevenueSum = totalRevenueSum.add(o.getTotalAmount());
                if (o.getDiscountAmount() != null) totalDiscountSum = totalDiscountSum.add(o.getDiscountAmount());
            }

            // Summary Row
            Row sumRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < 9; i++) {
                createCell(sumRow, i, i == 1 ? "TỔNG CỘNG (" + orders.size() + " ĐƠN)" : "", totalStyle);
            }
            createCell(sumRow, 9, "", totalStyle);
            createCell(sumRow, 10, totalDiscountSum, totalStyle);
            createCell(sumRow, 11, "", totalStyle);
            createCell(sumRow, 12, totalRevenueSum, totalStyle);
            createCell(sumRow, 13, "", totalStyle);

            ExcelExportHelper.autoSizeColumns(sheet, headers.length);

            return ExcelExportHelper.writeToByteArray(workbook);
        } catch (IOException e) {
            log.error("Failed to generate Orders Excel file", e);
            throw new RuntimeException("Không thể tạo file báo cáo Excel đơn hàng: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportInventoryReport(Integer warehouseId, String keyword, String stockStatus) {
        log.info("Generating Excel Inventory Report (warehouseId={}, status={}, keyword={})", warehouseId, stockStatus, keyword);
        InventoryFilterRequest request = InventoryFilterRequest.builder()
                .warehouseId(warehouseId)
                .keyword(keyword)
                .stockStatus(stockStatus != null ? stockStatus : "ALL")
                .page(0)
                .size(5000)
                .build();

        PageResponse<InventoryResponse> pageResponse = inventoryService.getInventoryPage(request);
        List<InventoryResponse> items = pageResponse.getContent();

        try (Workbook workbook = ExcelExportHelper.createWorkbook()) {
            CellStyle titleStyle = ExcelExportHelper.createTitleStyle(workbook);
            CellStyle subtitleStyle = ExcelExportHelper.createSubtitleStyle(workbook);
            CellStyle headerStyle = ExcelExportHelper.createHeaderStyle(workbook);
            CellStyle textStyle = ExcelExportHelper.createDataStyle(workbook, HorizontalAlignment.LEFT);
            CellStyle centerStyle = ExcelExportHelper.createDataStyle(workbook, HorizontalAlignment.CENTER);
            CellStyle currencyStyle = ExcelExportHelper.createCurrencyStyle(workbook);
            CellStyle integerStyle = ExcelExportHelper.createIntegerStyle(workbook);
            CellStyle totalStyle = ExcelExportHelper.createTotalStyle(workbook);

            Sheet sheet = workbook.createSheet("Báo cáo tồn kho");
            int rowIndex = 0;

            // Title
            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO TỒN KHO & CẢNH BÁO LINH KIỆN");
            titleCell.setCellStyle(titleStyle);

            Row subRow = sheet.createRow(rowIndex++);
            Cell subCell = subRow.createCell(0);
            subCell.setCellValue("Tổng số biến thể: " + items.size() + " | Thời gian xuất: " + LocalDateTime.now().format(DATE_TIME_FORMATTER));
            subCell.setCellStyle(subtitleStyle);
            rowIndex++;

            String[] headers = {
                    "STT", "Mã SKU", "Tên sản phẩm", "Tên biến thể / Quy cách", "Kho lưu trữ",
                    "Giá bán lẻ (VNĐ)", "Giá khuyến mãi (VNĐ)", "Tồn thực tế", "Đang giữ (Reserved)",
                    "Khả dụng (Available)", "Trạng thái kho", "Cập nhật lần cuối"
            };
            Row headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            int seq = 1;
            long totalPhysicalQty = 0;
            long totalReservedQty = 0;
            long totalAvailableQty = 0;

            for (InventoryResponse inv : items) {
                Row row = sheet.createRow(rowIndex++);
                createCell(row, 0, seq++, centerStyle);
                createCell(row, 1, inv.getSkuVariant() != null ? inv.getSkuVariant() : "", centerStyle);
                createCell(row, 2, inv.getProductName() != null ? inv.getProductName() : "", textStyle);
                createCell(row, 3, inv.getVariantName() != null ? inv.getVariantName() : "", textStyle);
                createCell(row, 4, inv.getWarehouseName() != null ? inv.getWarehouseName() : "", textStyle);
                createCell(row, 5, inv.getPrice() != null ? inv.getPrice() : BigDecimal.ZERO, currencyStyle);
                createCell(row, 6, inv.getSalePrice() != null ? inv.getSalePrice() : BigDecimal.ZERO, currencyStyle);
                createCell(row, 7, inv.getQuantity() != null ? inv.getQuantity() : 0, integerStyle);
                createCell(row, 8, inv.getReservedQty() != null ? inv.getReservedQty() : 0, integerStyle);
                createCell(row, 9, inv.getAvailableQty() != null ? inv.getAvailableQty() : 0, integerStyle);
                createCell(row, 10, inv.getStockStatus() != null ? inv.getStockStatus() : "", centerStyle);
                createCell(row, 11, inv.getUpdatedAt() != null ? inv.getUpdatedAt().format(DATE_TIME_FORMATTER) : "", centerStyle);

                if (inv.getQuantity() != null) totalPhysicalQty += inv.getQuantity();
                if (inv.getReservedQty() != null) totalReservedQty += inv.getReservedQty();
                if (inv.getAvailableQty() != null) totalAvailableQty += inv.getAvailableQty();
            }

            // Summary Row
            Row sumRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < 7; i++) {
                createCell(sumRow, i, i == 2 ? "TỔNG CỘNG" : "", totalStyle);
            }
            createCell(sumRow, 7, totalPhysicalQty, totalStyle);
            createCell(sumRow, 8, totalReservedQty, totalStyle);
            createCell(sumRow, 9, totalAvailableQty, totalStyle);
            createCell(sumRow, 10, "", totalStyle);
            createCell(sumRow, 11, "", totalStyle);

            ExcelExportHelper.autoSizeColumns(sheet, headers.length);

            return ExcelExportHelper.writeToByteArray(workbook);
        } catch (IOException e) {
            log.error("Failed to generate Inventory Excel file", e);
            throw new RuntimeException("Không thể tạo file báo cáo Excel tồn kho: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // HELPER CELL CREATION METHODS
    // ==========================================

    private void addKpiRow(Sheet sheet, int rowIndex, String label, Object value, CellStyle valueStyle, CellStyle labelStyle) {
        Row row = sheet.createRow(rowIndex);
        createCell(row, 0, label, labelStyle);
        if (value instanceof BigDecimal bd) {
            createCell(row, 1, bd, valueStyle);
        } else if (value instanceof Number num) {
            createCell(row, 1, num.longValue(), valueStyle);
        } else {
            createCell(row, 1, String.valueOf(value), valueStyle);
        }
    }

    private void createCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int colIndex, long value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int colIndex, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(0.0);
        }
        cell.setCellStyle(style);
    }
}
