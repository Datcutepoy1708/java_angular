package com.store.dto.response.inventory;

import com.store.entity.inventory.Warehouse;
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
public class WarehouseResponse {

    private Integer warehouseId;
    private String name;
    private String address;
    private String phone;

    public static WarehouseResponse fromEntity(Warehouse warehouse) {
        if (warehouse == null) return null;
        return WarehouseResponse.builder()
                .warehouseId(warehouse.getWarehouseId())
                .name(warehouse.getName())
                .address(warehouse.getAddress())
                .phone(warehouse.getPhone())
                .build();
    }
}
