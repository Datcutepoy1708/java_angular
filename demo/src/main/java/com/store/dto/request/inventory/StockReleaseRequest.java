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
public class StockReleaseRequest {

    @NotNull(message = "Variant ID is required")
    private Long variantId;

    @NotNull(message = "Warehouse ID is required")
    private Integer warehouseId;

    @NotNull(message = "Release quantity is required")
    @Min(value = 1, message = "Release quantity must be at least 1")
    private Integer quantity;

    private String referenceType;

    private Long referenceId;

    private String reason;
}
