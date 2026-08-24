package com.store.dto.request.inventory;

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
public class InventoryFilterRequest {

    private Integer warehouseId;

    private String keyword;

    /**
     * Filter stock status: "ALL", "IN_STOCK", "LOW_STOCK" (<= 10), "OUT_OF_STOCK" (<= 0)
     */
    @Builder.Default
    private String stockStatus = "ALL";

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private String sortBy = "updatedAt";

    @Builder.Default
    private String sortDir = "desc";
}
