package com.store.service;

import com.store.dto.request.inventory.StockDeductRequest;
import com.store.dto.request.inventory.StockReleaseRequest;
import com.store.dto.request.inventory.StockReserveRequest;
import com.store.entity.inventory.Inventory;
import com.store.entity.inventory.Warehouse;
import com.store.entity.product.ProductVariant;
import com.store.exception.InsufficientStockException;
import com.store.repository.InventoryLogRepository;
import com.store.repository.InventoryRepository;
import com.store.repository.ProductVariantRepository;
import com.store.repository.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
class InventoryConcurrencyStressIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private com.store.util.TestFixtureHelper fixtureHelper;

    private Long testVariantId;
    private Integer testWarehouseId;

    @BeforeEach
    void setUp() {
        fixtureHelper.ensureBasicFixtures();
        // Find existing variant and warehouse or create for test
        ProductVariant variant = productVariantRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No product variants found in database for test"));
        Warehouse warehouse = warehouseRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No warehouses found in database for test"));

        testVariantId = variant.getVariantId();
        testWarehouseId = warehouse.getWarehouseId();

        // Setup exact initial stock: 10 physical quantity, 0 reserved => exactly 10 available
        Inventory inventory = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, testWarehouseId)
                .orElseGet(() -> Inventory.builder()
                        .variant(variant)
                        .warehouse(warehouse)
                        .build());

        inventory.setQuantity(10);
        inventory.setReservedQty(0);
        inventoryRepository.saveAndFlush(inventory);
    }

    @AfterEach
    void tearDown() {
        // Reset stock to safe state
        Inventory inventory = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, testWarehouseId).orElse(null);
        if (inventory != null) {
            inventory.setReservedQty(0);
            inventoryRepository.saveAndFlush(inventory);
        }
    }

    @Test
    @DisplayName("Stress Test: 50 concurrent threads racing for 10 items must result in exactly 10 successes and 40 failures with 0 overselling")
    void testConcurrentStockReservations_NoOverselling() throws InterruptedException {
        int totalThreads = 50;
        int initialStock = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> unexpectedExceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < totalThreads; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    startSignal.await(); // Synchronize all threads to shoot at the exact same millisecond
                    StockReserveRequest request = StockReserveRequest.builder()
                            .variantId(testVariantId)
                            .warehouseId(testWarehouseId)
                            .quantity(1)
                            .referenceType("concurrent_test_order")
                            .referenceId((long) threadIndex)
                            .build();

                    inventoryService.reserveStock(request, null);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException ex) {
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    unexpectedExceptions.add(ex);
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startSignal.countDown();

        // Wait up to 15 seconds for all 50 threads to complete
        boolean finished = doneSignal.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(finished).isTrue();
        assertThat(unexpectedExceptions).isEmpty();
        assertThat(successCount.get()).isEqualTo(initialStock); // Exactly 10 must succeed
        assertThat(failureCount.get()).isEqualTo(totalThreads - initialStock); // Exactly 40 must fail

        // Verify final database state directly
        Inventory finalInventory = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, testWarehouseId)
                .orElseThrow();

        assertThat(finalInventory.getQuantity()).isEqualTo(10);
        assertThat(finalInventory.getReservedQty()).isEqualTo(10);
        assertThat(finalInventory.getAvailableQty()).isEqualTo(0); // Available must be exactly 0, NEVER negative
    }

    @Test
    @DisplayName("Integration Test: Complete 3-phase lifecycle (Reserve -> Deduct -> Release) against real DB")
    void testFullInventoryLifecycle_Reserve_Release_DeductCompletedStock() {
        // Initial state: quantity = 10, reserved = 0, available = 10
        // Step 1: Reserve 4 items for Order #100
        StockReserveRequest reserveReq = StockReserveRequest.builder()
                .variantId(testVariantId)
                .warehouseId(testWarehouseId)
                .quantity(4)
                .referenceType("ORDER")
                .referenceId(100L)
                .build();
        inventoryService.reserveStock(reserveReq, 1L);

        Inventory afterReserve = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, testWarehouseId).orElseThrow();
        assertThat(afterReserve.getQuantity()).isEqualTo(10);
        assertThat(afterReserve.getReservedQty()).isEqualTo(4);
        assertThat(afterReserve.getAvailableQty()).isEqualTo(6);

        // Step 2: Customer receives delivery -> Deduct 3 items completed
        com.store.dto.request.inventory.StockDeductRequest deductReq = com.store.dto.request.inventory.StockDeductRequest.builder()
                .variantId(testVariantId)
                .warehouseId(testWarehouseId)
                .quantity(3)
                .referenceType("ORDER")
                .referenceId(100L)
                .note("Đơn #100 giao thành công 3 món")
                .build();
        inventoryService.deductCompletedStock(deductReq, 1L);

        Inventory afterDeduct = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, testWarehouseId).orElseThrow();
        // Quantity decreased by 3 (10 - 3 = 7), reserved decreased by 3 (4 - 3 = 1)
        assertThat(afterDeduct.getQuantity()).isEqualTo(7);
        assertThat(afterDeduct.getReservedQty()).isEqualTo(1);
        assertThat(afterDeduct.getAvailableQty()).isEqualTo(6);

        // Step 3: 1 remaining item cancelled -> Release 1 item
        StockReleaseRequest releaseReq = StockReleaseRequest.builder()
                .variantId(testVariantId)
                .warehouseId(testWarehouseId)
                .quantity(1)
                .reason("Khách hủy 1 món còn lại")
                .build();
        inventoryService.releaseStock(releaseReq, 1L);

        Inventory afterRelease = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, testWarehouseId).orElseThrow();
        assertThat(afterRelease.getQuantity()).isEqualTo(7);
        assertThat(afterRelease.getReservedQty()).isEqualTo(0);
        assertThat(afterRelease.getAvailableQty()).isEqualTo(7);
    }
}
