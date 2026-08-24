package com.store.dto.response.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMetricsResponse {

    private long totalTrackedItems;
    private long lowStockItemsCount;
    private long outOfStockItemsCount;
    private long totalPhysicalQuantity;
}
