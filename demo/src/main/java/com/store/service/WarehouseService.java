package com.store.service;

import com.store.dto.response.inventory.WarehouseResponse;

import java.util.List;

public interface WarehouseService {
    List<WarehouseResponse> getAllWarehouses();
    WarehouseResponse getWarehouseById(Integer warehouseId);
}
