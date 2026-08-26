package com.store.service;

import com.store.dto.request.inventory.InventoryFilterRequest;
import com.store.dto.request.inventory.InventoryLogFilterRequest;
import com.store.dto.request.inventory.StockAdjustmentRequest;
import com.store.dto.request.inventory.StockImportRequest;
import com.store.dto.request.inventory.StockReleaseRequest;
import com.store.dto.request.inventory.StockReserveRequest;
import com.store.dto.request.inventory.StockTransferRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.inventory.InventoryLogResponse;
import com.store.dto.response.inventory.InventoryMetricsResponse;
import com.store.dto.response.inventory.InventoryResponse;
import com.store.dto.response.inventory.VariantStockSummaryResponse;

import java.util.List;

public interface InventoryService {

    PageResponse<InventoryResponse> getInventoryPage(InventoryFilterRequest request);

    VariantStockSummaryResponse getStockSummaryByVariant(Long variantId);

    List<VariantStockSummaryResponse> getStockSummaryByProduct(Long productId);

    InventoryResponse adjustStock(StockAdjustmentRequest request, Long currentUserId);

    List<InventoryResponse> importStock(StockImportRequest request, Long currentUserId);

    List<InventoryResponse> transferStock(StockTransferRequest request, Long currentUserId);

    InventoryResponse reserveStock(StockReserveRequest request, Long currentUserId);

    InventoryResponse releaseStock(StockReleaseRequest request, Long currentUserId);

    InventoryResponse deductCompletedStock(com.store.dto.request.inventory.StockDeductRequest request, Long currentUserId);

    PageResponse<InventoryResponse> getLowStockAlerts(int threshold, int page, int size);

    PageResponse<InventoryLogResponse> getInventoryLogs(InventoryLogFilterRequest request);

    InventoryMetricsResponse getInventoryMetrics();

    void restockReturnedItemAtomic(Long variantId, Integer warehouseId, int quantity, String itemCondition, Long returnId, String returnCode, Long currentUserId);
}
