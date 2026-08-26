package com.store.service;

import com.store.dto.request.inventory.StockAdjustmentRequest;
import com.store.dto.request.inventory.StockImportItemRequest;
import com.store.dto.request.inventory.StockImportRequest;
import com.store.dto.request.inventory.StockReleaseRequest;
import com.store.dto.request.inventory.StockReserveRequest;
import com.store.dto.request.inventory.StockTransferRequest;
import com.store.dto.response.inventory.InventoryResponse;
import com.store.dto.response.inventory.VariantStockSummaryResponse;
import com.store.entity.inventory.Inventory;
import com.store.entity.inventory.InventoryChangeType;
import com.store.entity.inventory.InventoryLog;
import com.store.entity.inventory.Warehouse;
import com.store.entity.product.Product;
import com.store.entity.product.ProductVariant;
import com.store.entity.user.User;
import com.store.exception.InsufficientStockException;
import com.store.repository.InventoryLogRepository;
import com.store.repository.InventoryRepository;
import com.store.repository.ProductVariantRepository;
import com.store.repository.UserRepository;
import com.store.repository.WarehouseRepository;
import com.store.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryLogRepository inventoryLogRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Warehouse warehouse1;
    private Warehouse warehouse2;
    private Product product;
    private ProductVariant variant;
    private Inventory inv1;
    private Inventory inv2;
    private User adminUser;

    @BeforeEach
    void setUp() {
        warehouse1 = Warehouse.builder()
                .warehouseId(1)
                .name("Kho Hà Nội")
                .address("120 Thái Hà")
                .phone("02438570512")
                .build();

        warehouse2 = Warehouse.builder()
                .warehouseId(2)
                .name("Kho TP.HCM")
                .address("215 Trần Quang Khải")
                .phone("02838206888")
                .build();

        product = Product.builder()
                .productId(100L)
                .name("MacBook Pro M4 14 inch")
                .slug("macbook-pro-m4-14-inch")
                .build();

        variant = ProductVariant.builder()
                .variantId(1L)
                .product(product)
                .variantName("16GB / 512GB Space Black")
                .skuVariant("MBP-M4-14-16-512-SB")
                .price(BigDecimal.valueOf(41990000))
                .build();

        inv1 = Inventory.builder()
                .inventoryId(10L)
                .variant(variant)
                .warehouse(warehouse1)
                .quantity(20)
                .reservedQty(2)
                .build();

        inv2 = Inventory.builder()
                .inventoryId(11L)
                .variant(variant)
                .warehouse(warehouse2)
                .quantity(15)
                .reservedQty(0)
                .build();

        adminUser = User.builder()
                .userId(1L)
                .fullName("Admin User")
                .email("admin@store.com")
                .build();
    }

    @Test
    @DisplayName("Should return stock summary across all warehouses accurately")
    void testGetStockSummaryByVariant() {
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findAllByOrderByNameAsc()).thenReturn(List.of(warehouse1, warehouse2));
        when(inventoryRepository.findByVariantVariantId(1L)).thenReturn(List.of(inv1, inv2));

        VariantStockSummaryResponse result = inventoryService.getStockSummaryByVariant(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTotalQuantity()).isEqualTo(35); // 20 + 15
        assertThat(result.getTotalReservedQty()).isEqualTo(2);  // 2 + 0
        assertThat(result.getTotalAvailableQty()).isEqualTo(33); // 35 - 2
        assertThat(result.isInStock()).isTrue();
        assertThat(result.getStockStatus()).isEqualTo("IN_STOCK");
        assertThat(result.getWarehouseBreakdown()).hasSize(2);
    }

    @Test
    @DisplayName("Should adjust stock quantity and write audit log")
    void testAdjustStock_Increase_Success() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .variantId(1L)
                .warehouseId(1)
                .quantityChange(5)
                .reason("Kiểm kê định kỳ phát hiện dư 5 chiếc")
                .build();

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(inventoryRepository.findByVariantIdAndWarehouseIdWithLock(1L, 1)).thenReturn(Optional.of(inv1));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryResponse response = inventoryService.adjustStock(request, 1L);

        assertThat(response).isNotNull();
        assertThat(inv1.getQuantity()).isEqualTo(25); // 20 + 5
        verify(inventoryLogRepository, times(1)).save(any(InventoryLog.class));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when reducing physical stock below reserved quantity")
    void testAdjustStock_BelowReserved_ThrowsException() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .variantId(1L)
                .warehouseId(1)
                .quantityChange(-19) // 20 - 19 = 1 < reserved (2)
                .reason("Giảm quá mức")
                .build();

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse1));
        when(inventoryRepository.findByVariantIdAndWarehouseIdWithLock(1L, 1)).thenReturn(Optional.of(inv1));

        assertThatThrownBy(() -> inventoryService.adjustStock(request, 1L))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Không thể giảm tồn kho xuống dưới số lượng đang được giữ cho đơn hàng");
    }

    @Test
    @DisplayName("Should import batch stock and record purchase_order audit logs")
    void testImportStock_Success() {
        StockImportRequest request = StockImportRequest.builder()
                .warehouseId(1)
                .supplierId(5L)
                .note("Nhập lô hàng chính hãng đợt 1")
                .items(List.of(
                        StockImportItemRequest.builder().variantId(1L).quantity(10).build()
                ))
                .build();

        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse1));
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(inventoryRepository.findByVariantIdAndWarehouseIdWithLock(1L, 1)).thenReturn(Optional.of(inv1));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<InventoryResponse> results = inventoryService.importStock(request, 1L);

        assertThat(results).hasSize(1);
        assertThat(inv1.getQuantity()).isEqualTo(30); // 20 + 10
        verify(inventoryLogRepository, times(1)).save(any(InventoryLog.class));
    }

    @Test
    @DisplayName("Should transfer stock between warehouses with 2 audit logs")
    void testTransferStock_Success() {
        StockTransferRequest request = StockTransferRequest.builder()
                .fromWarehouseId(1)
                .toWarehouseId(2)
                .variantId(1L)
                .quantity(5)
                .note("Điều phối tồn kho cho chi nhánh miền Nam")
                .build();

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse1));
        when(warehouseRepository.findById(2)).thenReturn(Optional.of(warehouse2));
        when(inventoryRepository.findByVariantIdAndWarehouseIdWithLock(1L, 1)).thenReturn(Optional.of(inv1));
        when(inventoryRepository.findByVariantIdAndWarehouseIdWithLock(1L, 2)).thenReturn(Optional.of(inv2));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<InventoryResponse> results = inventoryService.transferStock(request, 1L);

        assertThat(results).hasSize(2);
        assertThat(inv1.getQuantity()).isEqualTo(15); // 20 - 5
        assertThat(inv2.getQuantity()).isEqualTo(20); // 15 + 5
        verify(inventoryLogRepository, times(2)).save(any(InventoryLog.class));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when source warehouse lacks available stock for transfer")
    void testTransferStock_InsufficientSource_ThrowsException() {
        StockTransferRequest request = StockTransferRequest.builder()
                .fromWarehouseId(1)
                .toWarehouseId(2)
                .variantId(1L)
                .quantity(19) // Available in inv1 is 18 (20 - 2)
                .build();

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse1));
        when(warehouseRepository.findById(2)).thenReturn(Optional.of(warehouse2));
        when(inventoryRepository.findByVariantIdAndWarehouseIdWithLock(1L, 1)).thenReturn(Optional.of(inv1));

        assertThatThrownBy(() -> inventoryService.transferStock(request, 1L))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("không đủ tồn khả dụng để chuyển hàng");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when fromWarehouse equals toWarehouse")
    void testTransferStock_SameWarehouse_ThrowsException() {
        StockTransferRequest request = StockTransferRequest.builder()
                .fromWarehouseId(1)
                .toWarehouseId(1)
                .variantId(1L)
                .quantity(5)
                .build();

        assertThatThrownBy(() -> inventoryService.transferStock(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kho nguồn và kho đích không được trùng nhau");
    }

    @Test
    @DisplayName("Should reserve stock atomically when warehouse is specified")
    void testReserveStock_ExplicitWarehouse_Success() {
        StockReserveRequest request = StockReserveRequest.builder()
                .variantId(1L)
                .warehouseId(1)
                .quantity(3)
                .referenceType("order")
                .referenceId(101L)
                .build();

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(inventoryRepository.reserveStockAtomic(1L, 1, 3)).thenReturn(1);
        when(inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(1L, 1)).thenReturn(Optional.of(inv1));

        InventoryResponse response = inventoryService.reserveStock(request, 1L);

        assertThat(response).isNotNull();
        verify(inventoryRepository, times(1)).reserveStockAtomic(1L, 1, 3);
    }

    @Test
    @DisplayName("Should auto-allocate from priority warehouse when warehouseId is null")
    void testReserveStock_AutoAllocation_Success() {
        StockReserveRequest request = StockReserveRequest.builder()
                .variantId(1L)
                .warehouseId(null)
                .quantity(5)
                .build();

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(inventoryRepository.findAllByVariantIdWithLock(1L)).thenReturn(List.of(inv1, inv2));
        when(inventoryRepository.reserveStockAtomic(1L, 1, 5)).thenReturn(1);
        when(inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(1L, 1)).thenReturn(Optional.of(inv1));

        InventoryResponse response = inventoryService.reserveStock(request, 1L);

        assertThat(response).isNotNull();
        verify(inventoryRepository, times(1)).reserveStockAtomic(1L, 1, 5);
    }

    @Test
    @DisplayName("Should release reserved stock atomically")
    void testReleaseStock_Success() {
        StockReleaseRequest request = StockReleaseRequest.builder()
                .variantId(1L)
                .warehouseId(1)
                .quantity(2)
                .reason("Khách hủy đơn")
                .build();

        when(inventoryRepository.releaseStockAtomic(1L, 1, 2)).thenReturn(1);
        when(inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(1L, 1)).thenReturn(Optional.of(inv1));

        InventoryResponse response = inventoryService.releaseStock(request, 1L);

        assertThat(response).isNotNull();
        verify(inventoryRepository, times(1)).releaseStockAtomic(1L, 1, 2);
    }

    @Test
    @DisplayName("Should deduct completed stock atomically on order fulfillment and record SALE audit log")
    void testDeductCompletedStock_Success() {
        com.store.dto.request.inventory.StockDeductRequest request = com.store.dto.request.inventory.StockDeductRequest.builder()
                .variantId(1L)
                .warehouseId(1)
                .quantity(2)
                .referenceType("ORDER")
                .referenceId(555L)
                .note("Giao hàng thành công đơn #555")
                .build();

        when(inventoryRepository.deductCompletedStockAtomic(1L, 1, 2)).thenReturn(1);
        when(inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(1L, 1)).thenReturn(Optional.of(inv1));

        InventoryResponse response = inventoryService.deductCompletedStock(request, 1L);

        assertThat(response).isNotNull();
        verify(inventoryRepository, times(1)).deductCompletedStockAtomic(1L, 1, 2);
        verify(inventoryLogRepository, times(1)).save(any(InventoryLog.class));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when deductCompletedStockAtomic returns 0 rows")
    void testDeductCompletedStock_Insufficient_ThrowsException() {
        com.store.dto.request.inventory.StockDeductRequest request = com.store.dto.request.inventory.StockDeductRequest.builder()
                .variantId(1L)
                .warehouseId(1)
                .quantity(99)
                .build();

        when(inventoryRepository.deductCompletedStockAtomic(1L, 1, 99)).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.deductCompletedStock(request, 1L))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Số lượng tồn kho hoặc số lượng giữ chỗ không đủ để trừ hoàn tất đơn hàng");
    }

    @Test
    @DisplayName("restockReturnedItemAtomic should increase stock and log RETURN for salable condition (NEW_SEAL/OPENED)")
    void testRestockReturnedItemAtomic_Salable_Success() {
        when(inventoryRepository.increaseStockAtomic(1L, 1, 3)).thenReturn(1);
        when(productVariantRepository.getReferenceById(1L)).thenReturn(variant);
        when(warehouseRepository.getReferenceById(1)).thenReturn(warehouse1);
        when(userRepository.getReferenceById(10L)).thenReturn(adminUser);

        inventoryService.restockReturnedItemAtomic(1L, 1, 3, "OPENED", 100L, "RET-20260826-0001", 10L);

        verify(inventoryRepository).increaseStockAtomic(1L, 1, 3);
        verify(inventoryLogRepository).save(argThat(log ->
                log.getChangeType() == InventoryChangeType.RETURN &&
                log.getQuantityChange() == 3 &&
                log.getReferenceType().equals("RETURN_RMA")
        ));
    }

    @Test
    @DisplayName("restockReturnedItemAtomic should insert new Inventory if rows updated is 0")
    void testRestockReturnedItemAtomic_InsertIfNotExists_Success() {
        when(inventoryRepository.increaseStockAtomic(1L, 1, 2)).thenReturn(0);
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse1));
        when(productVariantRepository.getReferenceById(1L)).thenReturn(variant);
        when(warehouseRepository.getReferenceById(1)).thenReturn(warehouse1);

        inventoryService.restockReturnedItemAtomic(1L, 1, 2, "NEW_SEAL", 100L, "RET-20260826-0001", null);

        verify(inventoryRepository).save(argThat(inv ->
                inv.getQuantity() == 2 && inv.getReservedQty() == 0
        ));
        verify(inventoryLogRepository).save(any(InventoryLog.class));
    }

    @Test
    @DisplayName("restockReturnedItemAtomic should log ADJUST without increasing stock for DEFECTIVE/DAMAGED items")
    void testRestockReturnedItemAtomic_Defective_NoStockIncrease() {
        when(productVariantRepository.getReferenceById(1L)).thenReturn(variant);
        when(warehouseRepository.getReferenceById(1)).thenReturn(warehouse1);

        inventoryService.restockReturnedItemAtomic(1L, 1, 1, "DEFECTIVE", 100L, "RET-20260826-0001", null);

        verify(inventoryRepository, times(0)).increaseStockAtomic(anyLong(), any(), anyInt());
        verify(inventoryLogRepository).save(argThat(log ->
                log.getChangeType() == InventoryChangeType.ADJUST &&
                log.getQuantityChange() == 0 &&
                log.getNote().contains("bảo hành/NCC")
        ));
    }
}
