package com.store.dto.request.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class StockImportRequest {

    @NotNull(message = "Target Warehouse ID is required")
    private Integer warehouseId;

    private Long supplierId;

    private String note;

    @NotEmpty(message = "At least one item must be specified for stock import")
    @Valid
    private List<StockImportItemRequest> items;
}
