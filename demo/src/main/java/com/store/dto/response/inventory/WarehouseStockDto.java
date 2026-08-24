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
public class WarehouseStockDto {

    private Integer warehouseId;
    private String warehouseName;
    private String warehouseAddress;
    private Integer quantity;
    private Integer reservedQty;
    private Integer availableQty;
}
