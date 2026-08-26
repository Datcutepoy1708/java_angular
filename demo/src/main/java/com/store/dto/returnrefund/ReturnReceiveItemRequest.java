package com.store.dto.returnrefund;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnReceiveItemRequest {

    @NotNull(message = "Kho nhận hàng hoàn không được để trống")
    private Integer warehouseId;

    private String adminNote;

    private List<ItemConditionUpdate> itemConditions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemConditionUpdate {
        private Long returnItemId;
        private String condition; // NEW_SEAL, OPENED, DAMAGED, DEFECTIVE
    }
}
