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
public class StockReserveRequest {

    @NotNull(message = "Variant ID is required")
    private Long variantId;

    /**
     * Optional explicit warehouse ID.
     * If null, system automatically allocates from priority warehouses (1 -> 2 -> 3).
     */
    private Integer warehouseId;

    @NotNull(message = "Reserve quantity is required")
    @Min(value = 1, message = "Reserve quantity must be at least 1")
    private Integer quantity;

    private String referenceType;

    private Long referenceId;
}
