package com.store.service;

import com.store.dto.response.inventory.WarehouseResponse;
import com.store.entity.inventory.Warehouse;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.WarehouseRepository;
import com.store.service.impl.WarehouseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    private Warehouse warehouseHanoi;
    private Warehouse warehouseHcm;

    @BeforeEach
    void setUp() {
        warehouseHanoi = Warehouse.builder()
                .warehouseId(1)
                .name("Kho Tổng Miền Bắc (Hà Nội)")
                .address("120 Thái Hà, Đống Đa, Hà Nội")
                .phone("02438570512")
                .build();

        warehouseHcm = Warehouse.builder()
                .warehouseId(2)
                .name("Kho Tổng Miền Nam (TP.HCM)")
                .address("215 Trần Quang Khải, Quận 1, TP.HCM")
                .phone("02838206888")
                .build();
    }

    @Test
    @DisplayName("Should return all active warehouses sorted by name")
    void testGetAllWarehouses() {
        when(warehouseRepository.findAllByOrderByNameAsc()).thenReturn(List.of(warehouseHanoi, warehouseHcm));

        List<WarehouseResponse> result = warehouseService.getAllWarehouses();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Kho Tổng Miền Bắc (Hà Nội)");
        assertThat(result.get(1).getName()).isEqualTo("Kho Tổng Miền Nam (TP.HCM)");
        verify(warehouseRepository, times(1)).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("Should return warehouse by ID when found")
    void testGetWarehouseById_Success() {
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouseHanoi));

        WarehouseResponse result = warehouseService.getWarehouseById(1);

        assertThat(result).isNotNull();
        assertThat(result.getWarehouseId()).isEqualTo(1);
        assertThat(result.getName()).isEqualTo("Kho Tổng Miền Bắc (Hà Nội)");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when warehouse ID not found")
    void testGetWarehouseById_NotFound() {
        when(warehouseRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.getWarehouseById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Warehouse not found with id: 99");
    }
}
