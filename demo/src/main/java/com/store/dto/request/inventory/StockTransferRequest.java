package com.store.dto.request.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class StockTransferRequest {

    @NotNull(message = "Source Warehouse ID (fromWarehouseId) is required")
    private Integer fromWarehouseId;

    @NotNull(message = "Destination Warehouse ID (toWarehouseId) is required")
    private Integer toWarehouseId;

    @NotNull(message = "Variant ID is required")
    private Long variantId;

    @NotNull(message = "Quantity to transfer is required")
    @Min(value = 1, message = "Transfer quantity must be at least 1")
    private Integer quantity;

    private String note;
}
