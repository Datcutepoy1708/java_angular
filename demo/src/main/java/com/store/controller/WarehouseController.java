package com.store.controller;

import com.store.dto.response.ApiResponse;
import com.store.dto.response.inventory.WarehouseResponse;
import com.store.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouse", description = "Warehouse locations and details APIs")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    @Operation(summary = "Get all active warehouses")
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> getAllWarehouses() {
        return ResponseEntity.ok(ApiResponse.success("Warehouses retrieved successfully", warehouseService.getAllWarehouses()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get warehouse by ID")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getWarehouseById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Warehouse retrieved successfully", warehouseService.getWarehouseById(id)));
    }
}
