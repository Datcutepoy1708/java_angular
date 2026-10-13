package com.store.controller;

import com.store.dto.request.order.OrderFilterRequest;
import com.store.service.ReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
@Tag(name = "Admin Report Export", description = "Excel & Document Export APIs for Admin Management")
public class ReportExportController {

    private final ReportExportService reportExportService;

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @GetMapping("/revenue/excel")
    @Operation(summary = "Export Revenue & KPI analytics to formatted Excel (.xlsx)")
    public ResponseEntity<byte[]> exportRevenueExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "day") String period
    ) {
        byte[] content = reportExportService.exportRevenueReport(startDate, endDate, period);
        String filename = "Bao_cao_doanh_thu_" + LocalDate.now().toString() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                .body(content);
    }

    @GetMapping("/orders/excel")
    @Operation(summary = "Export Filtered Orders list to formatted Excel (.xlsx)")
    public ResponseEntity<byte[]> exportOrdersExcel(
            @ModelAttribute OrderFilterRequest filter
    ) {
        byte[] content = reportExportService.exportOrdersReport(filter);
        String filename = "Danh_sach_don_hang_" + LocalDate.now().toString() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                .body(content);
    }

    @GetMapping("/inventory/excel")
    @Operation(summary = "Export Warehouse Inventory report to formatted Excel (.xlsx)")
    public ResponseEntity<byte[]> exportInventoryExcel(
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String stockStatus
    ) {
        byte[] content = reportExportService.exportInventoryReport(warehouseId, keyword, stockStatus);
        String filename = "Bao_cao_ton_kho_" + LocalDate.now().toString() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                .body(content);
    }
}
