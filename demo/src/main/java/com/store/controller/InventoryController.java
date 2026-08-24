package com.store.controller;

import com.store.dto.request.inventory.InventoryFilterRequest;
import com.store.dto.request.inventory.InventoryLogFilterRequest;
import com.store.dto.request.inventory.StockAdjustmentRequest;
import com.store.dto.request.inventory.StockImportRequest;
import com.store.dto.request.inventory.StockReleaseRequest;
import com.store.dto.request.inventory.StockReserveRequest;
import com.store.dto.request.inventory.StockTransferRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.PageResponse;
import com.store.dto.response.inventory.InventoryLogResponse;
import com.store.dto.response.inventory.InventoryMetricsResponse;
import com.store.dto.response.inventory.InventoryResponse;
import com.store.dto.response.inventory.VariantStockSummaryResponse;
import com.store.security.CustomUserDetails;
import com.store.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Multi-Warehouse Inventory & Real-Time Stock APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get paginated inventory items with multi-criteria filters")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getInventoryPage(
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String stockStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        InventoryFilterRequest request = InventoryFilterRequest.builder()
                .warehouseId(warehouseId)
                .keyword(keyword)
                .stockStatus(stockStatus)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Inventory page retrieved successfully", inventoryService.getInventoryPage(request)));
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get overall inventory metrics for KPI cards")
    public ResponseEntity<ApiResponse<InventoryMetricsResponse>> getInventoryMetrics() {
        return ResponseEntity.ok(ApiResponse.success("Inventory metrics calculated", inventoryService.getInventoryMetrics()));
    }

    @GetMapping("/variants/{variantId}/summary")
    @Operation(summary = "Get real-time stock summary across all warehouses for a variant")
    public ResponseEntity<ApiResponse<VariantStockSummaryResponse>> getStockSummaryByVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getStockSummaryByVariant(variantId)));
    }

    @GetMapping("/products/{productId}/stock")
    @Operation(summary = "Get real-time stock summary for all variants of a product")
    public ResponseEntity<ApiResponse<List<VariantStockSummaryResponse>>> getStockSummaryByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getStockSummaryByProduct(productId)));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually adjust inventory quantity (mandatory audit reason)")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        InventoryResponse response = inventoryService.adjustStock(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Tồn kho đã được điều chỉnh thành công", response));
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Import stock to warehouse from supplier")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> importStock(
            @Valid @RequestBody StockImportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        List<InventoryResponse> response = inventoryService.importStock(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Phiếu nhập kho đã được xử lý thành công", response));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Transfer stock between warehouses")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> transferStock(
            @Valid @RequestBody StockTransferRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        List<InventoryResponse> response = inventoryService.transferStock(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Hàng đã được chuyển kho thành công", response));
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Internal API: Reserve stock for order (Phase 5 prep)",
            description = "Reserved for internal order processing in Phase 5. Currently restricted to ADMIN for integration testing."
    )
    public ResponseEntity<ApiResponse<InventoryResponse>> reserveStock(
            @Valid @RequestBody StockReserveRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        InventoryResponse response = inventoryService.reserveStock(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Giữ hàng thành công", response));
    }

    @PostMapping("/release")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Internal API: Release reserved stock (Phase 5 prep)",
            description = "Reserved for internal order cancellation in Phase 5. Currently restricted to ADMIN for integration testing."
    )
    public ResponseEntity<ApiResponse<InventoryResponse>> releaseStock(
            @Valid @RequestBody StockReleaseRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        InventoryResponse response = inventoryService.releaseStock(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Hoàn trả giữ hàng thành công", response));
    }

    @PostMapping("/deduct")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Internal API: Deduct completed stock on order fulfillment (Phase 5 prep)",
            description = "Reserved for internal order completion/delivery in Phase 5. Currently restricted to ADMIN for integration testing."
    )
    public ResponseEntity<ApiResponse<InventoryResponse>> deductCompletedStock(
            @Valid @RequestBody com.store.dto.request.inventory.StockDeductRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        InventoryResponse response = inventoryService.deductCompletedStock(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Trừ tồn kho hoàn tất đơn hàng thành công", response));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get list of low stock items nearing exhaustion")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getLowStockAlerts(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Low stock alerts retrieved", inventoryService.getLowStockAlerts(threshold, page, size)));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get inventory audit logs with filtering and pagination")
    public ResponseEntity<ApiResponse<PageResponse<InventoryLogResponse>>> getInventoryLogs(
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        InventoryLogFilterRequest request = InventoryLogFilterRequest.builder()
                .variantId(variantId)
                .warehouseId(warehouseId)
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", inventoryService.getInventoryLogs(request)));
    }
}
