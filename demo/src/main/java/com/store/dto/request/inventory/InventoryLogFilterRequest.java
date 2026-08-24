package com.store.dto.request.inventory;

import com.store.entity.inventory.InventoryChangeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLogFilterRequest {

    private Long variantId;

    private Integer warehouseId;

    private InventoryChangeType changeType;

    private String keyword;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
