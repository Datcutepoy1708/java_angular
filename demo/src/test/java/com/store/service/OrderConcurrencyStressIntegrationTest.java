package com.store.service;

import com.store.dto.request.cart.AddToCartRequest;
import com.store.dto.request.order.CreateOrderRequest;
import com.store.dto.response.order.OrderResponse;
import com.store.entity.inventory.Inventory;
import com.store.entity.inventory.Warehouse;
import com.store.entity.order.PaymentMethod;
import com.store.entity.product.ProductVariant;
import com.store.entity.user.User;
import com.store.exception.InsufficientStockException;
import com.store.repository.CartItemRepository;
import com.store.repository.InventoryRepository;
import com.store.repository.OrderRepository;
import com.store.repository.ProductVariantRepository;
import com.store.repository.UserRepository;
import com.store.repository.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
class OrderConcurrencyStressIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private com.store.util.TestFixtureHelper fixtureHelper;

    private Long testVariantId;
    private Integer testWarehouseId;
    private List<Long> testUserIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        fixtureHelper.ensureBasicFixtures();
        ProductVariant variant = productVariantRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No variants found for test"));
        Warehouse warehouse = warehouseRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No warehouses found for test"));

        testVariantId = variant.getVariantId();
        testWarehouseId = warehouse.getWarehouseId();

        // Zero out stock in all warehouses for this variant first to prevent bleed from other warehouses
        List<Inventory> existingInventories = inventoryRepository.findAll().stream()
                .filter(inv -> inv.getVariant().getVariantId().equals(testVariantId))
                .toList();
        for (Inventory inv : existingInventories) {
            inv.setQuantity(0);
            inv.setReservedQty(0);
            inventoryRepository.save(inv);
        }
        inventoryRepository.flush();

        // Exact stock: 5 quantity, 0 reserved => exactly 5 available in test warehouse
        Inventory inventory = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, testWarehouseId)
                .orElseGet(() -> Inventory.builder().variant(variant).warehouse(warehouse).build());

        inventory.setQuantity(5);
        inventory.setReservedQty(0);
        inventoryRepository.saveAndFlush(inventory);

        // Prepare 20 distinct test users with 1 item in cart each
        testUserIds.clear();
        for (int i = 1; i <= 20; i++) {
            String email = "stress_buyer_" + i + "@store.com";
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User u = User.builder()
                        .fullName("Stress Buyer " + System.currentTimeMillis())
                        .email(email)
                        .phone("098" + String.format("%07d", System.nanoTime() % 10000000))
                        .passwordHash("$2a$10$e7K...")
                        .build();
                return userRepository.saveAndFlush(u);
            });
            testUserIds.add(user.getUserId());
            cartService.clearCart(user.getUserId());
            cartService.addToCart(user.getUserId(), new AddToCartRequest(testVariantId, 1));
        }
    }

    @AfterEach
    void tearDown() {
        for (Long uid : testUserIds) {
            cartService.clearCart(uid);
        }
        Inventory inventory = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, testWarehouseId).orElse(null);
        if (inventory != null) {
            inventory.setReservedQty(0);
            inventoryRepository.saveAndFlush(inventory);
        }
    }

    @Test
    @DisplayName("Stress Test: 20 concurrent threads racing for 5 available stock items in checkout must yield exactly 5 orders and 0 overselling")
    void testConcurrentOrderPlacement_NoOverselling_UniqueOrderCodes() throws InterruptedException {
        int totalThreads = 20;
        int availableStock = 5;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<OrderResponse> createdOrders = Collections.synchronizedList(new ArrayList<>());
        List<Exception> unexpectedExceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < totalThreads; i++) {
            final Long currentUserId = testUserIds.get(i);
            executor.submit(() -> {
                try {
                    startSignal.await();
                    CreateOrderRequest request = CreateOrderRequest.builder()
                            .receiverName("Test Concurrency Receiver " + currentUserId)
                            .receiverPhone("0987654321")
                            .shippingAddress("123 Phố Test, Hà Nội")
                            .paymentMethod(PaymentMethod.COD)
                            .build();

                    OrderResponse order = orderService.createOrder(currentUserId, request);
                    createdOrders.add(order);
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

        // Fire all 20 threads simultaneously
        startSignal.countDown();

        boolean completed = doneSignal.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        Inventory finalInventory = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, testWarehouseId).orElseThrow();

        System.out.println("================================================================================");
        System.out.println("[CONCURRENCY STRESS TEST RESULTS]");
        System.out.println("  Total Competing Threads: " + totalThreads);
        System.out.println("  Initial Available Stock: " + availableStock);
        System.out.println("  Successful Orders Placed: " + successCount.get());
        System.out.println("  Failed Orders (Stock Exceeded): " + failureCount.get());
        System.out.println("  Final Quantity in Warehouse: " + finalInventory.getQuantity());
        System.out.println("  Final Reserved Quantity: " + finalInventory.getReservedQty());
        System.out.println("  Final Available Quantity: " + finalInventory.getAvailableQty());
        System.out.println("  Created Order Codes (" + createdOrders.size() + " total):");
        for (OrderResponse ord : createdOrders) {
            System.out.println("    - " + ord.getOrderCode() + " | Total: " + ord.getTotalAmount() + " VND | Status: " + ord.getOrderStatus());
        }
        System.out.println("================================================================================");

        assertThat(completed).isTrue();
        assertThat(unexpectedExceptions).isEmpty();

        // Exact assertions: Exactly 5 succeed, exactly 15 fail, 0 available stock remaining
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failureCount.get()).isEqualTo(15);
        assertThat(successCount.get() + failureCount.get()).isEqualTo(totalThreads);

        // Check uniqueness of generated order codes
        Set<String> orderCodes = new HashSet<>();
        for (OrderResponse order : createdOrders) {
            assertThat(orderCodes.add(order.getOrderCode()))
                    .as("Order code must be unique: " + order.getOrderCode())
                    .isTrue();
        }

        // Verify stock in database
        assertThat(finalInventory.getQuantity()).isEqualTo(5);
        assertThat(finalInventory.getReservedQty()).isEqualTo(5);
        assertThat(finalInventory.getAvailableQty()).isEqualTo(0); // EXACTLY ZERO, NEVER NEGATIVE
    }

    @Test
    @DisplayName("DB Rollback Test: When 1 of 3 items in cart fails due to InsufficientStock, Spring @Transactional must rollback DB state so previous items have reserved_qty=0, order table has 0 garbage rows, and cart is intact")
    void testCreateOrder_PartialReserve_RollsBackOnDbLevel() {
        fixtureHelper.ensureBasicFixtures();
        List<ProductVariant> variants = productVariantRepository.findAll();
        assertThat(variants.size()).isGreaterThanOrEqualTo(3);

        ProductVariant variant1 = variants.get(0);
        ProductVariant variant2 = variants.get(1);
        ProductVariant variant3 = variants.get(2);

        Warehouse warehouse = warehouseRepository.findAll().stream().findFirst().orElseThrow();
        Integer warehouseId = warehouse.getWarehouseId();

        // Setup stock: variant1 = 10 (available 10), variant2 = 10 (available 10), variant3 = 0 (available 0)
        Inventory inv1 = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(variant1.getVariantId(), warehouseId)
                .orElseGet(() -> Inventory.builder().variant(variant1).warehouse(warehouse).build());
        inv1.setQuantity(10);
        inv1.setReservedQty(0);
        inventoryRepository.save(inv1);

        Inventory inv2 = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(variant2.getVariantId(), warehouseId)
                .orElseGet(() -> Inventory.builder().variant(variant2).warehouse(warehouse).build());
        inv2.setQuantity(10);
        inv2.setReservedQty(0);
        inventoryRepository.save(inv2);

        // Zero out variant3 in ALL warehouses so it has 0 available
        for (Inventory inv : inventoryRepository.findAll()) {
            if (inv.getVariant().getVariantId().equals(variant3.getVariantId())) {
                inv.setQuantity(0);
                inv.setReservedQty(0);
                inventoryRepository.save(inv);
            }
        }
        inventoryRepository.flush();

        Long buyerId = testUserIds.get(0);
        cartService.clearCart(buyerId);
        cartService.addToCart(buyerId, new AddToCartRequest(variant1.getVariantId(), 1));
        cartService.addToCart(buyerId, new AddToCartRequest(variant2.getVariantId(), 1));
        cartService.addToCart(buyerId, new AddToCartRequest(variant3.getVariantId(), 2));

        long initialOrderCount = orderRepository.count();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .receiverName("Rollback Test Buyer")
                .receiverPhone("0987654321")
                .shippingAddress("123 Phố Rollback, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .build();

        // Execution: calling createOrder MUST throw InsufficientStockException
        org.junit.jupiter.api.Assertions.assertThrows(InsufficientStockException.class, () -> {
            orderService.createOrder(buyerId, request);
        });

        // ══════════════════════════════════════════════════════════════
        // VERIFY REAL DATABASE STATE AFTER TRANSACTION ROLLBACK
        // ══════════════════════════════════════════════════════════════
        
        // 1. Check Item 1: reserved_qty must be 0 (the atomic reservation was rolled back in MySQL!)
        Inventory verifyInv1 = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(variant1.getVariantId(), warehouseId).orElseThrow();
        assertThat(verifyInv1.getQuantity()).isEqualTo(10);
        assertThat(verifyInv1.getReservedQty()).isEqualTo(0);
        assertThat(verifyInv1.getAvailableQty()).isEqualTo(10);

        // 2. Check Item 2: reserved_qty must be 0 (the atomic reservation was rolled back in MySQL!)
        Inventory verifyInv2 = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(variant2.getVariantId(), warehouseId).orElseThrow();
        assertThat(verifyInv2.getQuantity()).isEqualTo(10);
        assertThat(verifyInv2.getReservedQty()).isEqualTo(0);
        assertThat(verifyInv2.getAvailableQty()).isEqualTo(10);

        // 3. Check Order table: count must NOT have increased (0 garbage orders created)
        assertThat(orderRepository.count()).isEqualTo(initialOrderCount);

        // 4. Check Cart table: user still has all 3 items in cart (not deleted/cleared)
        var userCart = cartItemRepository.findByUserIdWithDetails(buyerId);
        assertThat(userCart).hasSize(3);

        System.out.println("================================================================================");
        System.out.println("[TRANSACTIONAL ROLLBACK TEST RESULTS (DB LEVEL)]");
        System.out.println("  Item 1 Variant ID: " + variant1.getVariantId() + " -> Quantity: " + verifyInv1.getQuantity() + ", Reserved: " + verifyInv1.getReservedQty() + " (ROLLED BACK TO 0)");
        System.out.println("  Item 2 Variant ID: " + variant2.getVariantId() + " -> Quantity: " + verifyInv2.getQuantity() + ", Reserved: " + verifyInv2.getReservedQty() + " (ROLLED BACK TO 0)");
        System.out.println("  Item 3 Variant ID: " + variant3.getVariantId() + " -> Insufficient stock triggered rollback");
        System.out.println("  Orders in DB: " + orderRepository.count() + " (Initial: " + initialOrderCount + ", Zero phantom orders created)");
        System.out.println("  Cart items preserved: " + userCart.size() + " items intact in cart");
        System.out.println("================================================================================");
    }
}
