package com.store.dto.response.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantStockSummaryResponse {

    private Long variantId;
    private String variantName;
    private String skuVariant;
    private Long productId;
    private String productName;
    private Integer totalQuantity;
    private Integer totalReservedQty;
    private Integer totalAvailableQty;
    private boolean inStock;
    private String stockStatus; // "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"
    private List<WarehouseStockDto> warehouseBreakdown;
}
