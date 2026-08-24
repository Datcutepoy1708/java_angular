package com.store.service.impl;

import com.store.dto.response.inventory.WarehouseResponse;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.WarehouseRepository;
import com.store.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAllWarehouses() {
        log.info("Fetching all active warehouses");
        return warehouseRepository.findAllByOrderByNameAsc().stream()
                .map(WarehouseResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Integer warehouseId) {
        log.info("Fetching warehouse with id: {}", warehouseId);
        return warehouseRepository.findById(warehouseId)
                .map(WarehouseResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));
    }
}
