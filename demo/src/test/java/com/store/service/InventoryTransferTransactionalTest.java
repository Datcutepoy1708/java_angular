package com.store.service;

import com.store.dto.request.inventory.StockTransferRequest;
import com.store.entity.inventory.Inventory;
import com.store.entity.inventory.Warehouse;
import com.store.entity.product.ProductVariant;
import com.store.repository.InventoryLogRepository;
import com.store.repository.InventoryRepository;
import com.store.repository.ProductVariantRepository;
import com.store.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
class InventoryTransferTransactionalTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @SpyBean
    private InventoryLogRepository inventoryLogRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long testVariantId;
    private Integer sourceWarehouseId;
    private Integer targetWarehouseId;

    @BeforeEach
    void setUp() {
        ProductVariant variant = productVariantRepository.findAll().stream().findFirst().orElseThrow();
        List<Warehouse> warehouses = warehouseRepository.findAll();
        assertThat(warehouses.size()).isGreaterThanOrEqualTo(2);

        testVariantId = variant.getVariantId();
        sourceWarehouseId = warehouses.get(0).getWarehouseId();
        targetWarehouseId = warehouses.get(1).getWarehouseId();

        // Setup source with 30 items, target with 10 items
        Inventory sourceInv = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, sourceWarehouseId)
                .orElseGet(() -> Inventory.builder().variant(variant).warehouse(warehouses.get(0)).build());
        sourceInv.setQuantity(30);
        sourceInv.setReservedQty(0);
        inventoryRepository.saveAndFlush(sourceInv);

        Inventory targetInv = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, targetWarehouseId)
                .orElseGet(() -> Inventory.builder().variant(variant).warehouse(warehouses.get(1)).build());
        targetInv.setQuantity(10);
        targetInv.setReservedQty(0);
        inventoryRepository.saveAndFlush(targetInv);
    }

    @Test
    @DisplayName("Happy Path: Successful transfer updates both warehouses and creates 2 audit logs")
    void testTransferStock_HappyPath() {
        long initialLogsCount = inventoryLogRepository.count();

        StockTransferRequest request = StockTransferRequest.builder()
                .fromWarehouseId(sourceWarehouseId)
                .toWarehouseId(targetWarehouseId)
                .variantId(testVariantId)
                .quantity(10)
                .note("Transfer 10 units test")
                .build();

        inventoryService.transferStock(request, null);

        Inventory finalSource = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, sourceWarehouseId).orElseThrow();
        Inventory finalTarget = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, targetWarehouseId).orElseThrow();

        assertThat(finalSource.getQuantity()).isEqualTo(20); // 30 - 10
        assertThat(finalTarget.getQuantity()).isEqualTo(20); // 10 + 10
        assertThat(inventoryLogRepository.count()).isEqualTo(initialLogsCount + 2);
    }

    @Test
    @DisplayName("Rollback Test: When failure occurs mid-transaction during log creation, both warehouses must rollback 100%")
    void testTransferStock_RollbackOnMidTransactionFailure() {
        long initialLogsCount = inventoryLogRepository.count();

        // Simulate crash right when saving the second audit log (after source & target have been modified in memory)
        doAnswer(invocation -> {
            throw new RuntimeException("Simulated mid-transaction database crash after source deduction");
        }).when(inventoryLogRepository).save(any());

        StockTransferRequest request = StockTransferRequest.builder()
                .fromWarehouseId(sourceWarehouseId)
                .toWarehouseId(targetWarehouseId)
                .variantId(testVariantId)
                .quantity(10)
                .note("Transfer doomed to fail")
                .build();

        assertThatThrownBy(() -> inventoryService.transferStock(request, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated mid-transaction database crash");

        // Verify in a fresh isolated read transaction that database rolled back to original numbers
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        txTemplate.execute(status -> {
            Inventory rolledBackSource = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, sourceWarehouseId).orElseThrow();
            Inventory rolledBackTarget = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, targetWarehouseId).orElseThrow();

            assertThat(rolledBackSource.getQuantity()).isEqualTo(30); // Source MUST NOT stay deducted
            assertThat(rolledBackTarget.getQuantity()).isEqualTo(10); // Target MUST NOT stay credited
            assertThat(inventoryLogRepository.count()).isEqualTo(initialLogsCount); // 0 logs persisted
            return null;
        });
    }
}
