package com.store.service;

import com.store.dto.request.cart.AddToCartRequest;
import com.store.dto.request.order.CreateOrderRequest;
import com.store.dto.response.order.OrderResponse;
import com.store.entity.discount.DiscountCode;
import com.store.entity.discount.DiscountStatus;
import com.store.entity.discount.DiscountType;
import com.store.entity.inventory.Inventory;
import com.store.entity.inventory.Warehouse;
import com.store.entity.order.PaymentMethod;
import com.store.entity.product.ProductVariant;
import com.store.entity.user.User;
import com.store.exception.InvalidDiscountException;
import com.store.repository.CartItemRepository;
import com.store.repository.DiscountCodeRepository;
import com.store.repository.DiscountUsageRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class DiscountConcurrencyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private DiscountCodeRepository discountCodeRepository;

    @Autowired
    private DiscountUsageRepository discountUsageRepository;

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
    private Long testDiscountId;
    private String testDiscountCode;

    @BeforeEach
    void setUp() {
        fixtureHelper.ensureBasicFixtures();
        ProductVariant variant = productVariantRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No variants found for test"));
        Warehouse warehouse = warehouseRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No warehouses found for test"));

        testVariantId = variant.getVariantId();
        testWarehouseId = warehouse.getWarehouseId();

        // Setup ample stock (50 items) so stock is never the bottleneck
        List<Inventory> existingInventories = inventoryRepository.findAll().stream()
                .filter(inv -> inv.getVariant().getVariantId().equals(testVariantId))
                .toList();

        for (Inventory inv : existingInventories) {
            inv.setQuantity(0);
            inv.setReservedQty(0);
            inventoryRepository.save(inv);
        }

        Inventory targetInventory = existingInventories.stream()
                .filter(inv -> inv.getWarehouse().getWarehouseId().equals(testWarehouseId))
                .findFirst()
                .orElseGet(() -> Inventory.builder()
                        .variant(variant)
                        .warehouse(warehouse)
                        .quantity(0)
                        .reservedQty(0)
                        .build());

        targetInventory.setQuantity(50);
        targetInventory.setReservedQty(0);
        inventoryRepository.save(targetInventory);

        // Create unique discount code per test run
        testDiscountCode = "CONCURRENT_" + System.nanoTime();
        DiscountCode discount = DiscountCode.builder()
                .code(testDiscountCode)
                .description("Mã giảm giá concurrency test: chỉ có 5 lượt")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("100000.00"))
                .minOrderValue(BigDecimal.ZERO)
                .usageLimit(5)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .status(DiscountStatus.ACTIVE)
                .build();
        DiscountCode savedDiscount = discountCodeRepository.save(discount);
        testDiscountId = savedDiscount.getDiscountId();

        // Create 20 distinct users for 20 threads
        testUserIds.clear();
        for (int i = 1; i <= 20; i++) {
            String email = "discount_concurrency_user_" + i + "@test.com";
            final int index = i;
            User user = userRepository.findByEmail(email).orElseGet(() ->
                    userRepository.save(User.builder()
                            .email(email)
                            .passwordHash("$2a$10$dummyHashForTestingPurposesOnly1234567890")
                            .fullName("Discount Concurrency Tester " + index)
                            .phone("09870000" + (index < 10 ? "0" + index : index))
                            .build())
            );
            testUserIds.add(user.getUserId());
        }
    }

    @AfterEach
    void tearDown() {
        for (Long userId : testUserIds) {
            cartService.clearCart(userId);
        }
    }

    @Test
    @DisplayName("Stress Test: 20 concurrent threads racing for 5 coupon usages -> exactly 5 succeed, 15 fail, used_count = 5")
    void testConcurrentDiscountUsage_ExactlyLimitSucceeds_RestFail() throws Exception {
        int totalThreads = 20;
        int expectedSuccess = 5;
        int expectedFailures = totalThreads - expectedSuccess; // 15

        // Seed 1 item into each user's cart
        for (Long userId : testUserIds) {
            cartService.clearCart(userId);
            cartService.addToCart(userId, AddToCartRequest.builder()
                    .variantId(testVariantId)
                    .quantity(1)
                    .build());
        }

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<OrderResponse> createdOrders = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> unexpectedExceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < totalThreads; i++) {
            final Long currentUserId = testUserIds.get(i);
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Wait for simultaneous release

                    CreateOrderRequest req = CreateOrderRequest.builder()
                            .receiverName("Tester")
                            .receiverPhone("0987654321")
                            .shippingAddress("123 Test Street, Ha Noi")
                            .paymentMethod(PaymentMethod.COD)
                            .discountCode(testDiscountCode)
                            .note("Discount concurrency stress test order")
                            .build();

                    OrderResponse response = orderService.createOrder(currentUserId, req);
                    successCount.incrementAndGet();
                    createdOrders.add(response);
                } catch (InvalidDiscountException e) {
                    failureCount.incrementAndGet();
                } catch (Throwable t) {
                    unexpectedExceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire all 20 threads simultaneously
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(unexpectedExceptions).isEmpty();

        // Exact assertions: Exactly 5 succeed, exactly 15 fail with InvalidDiscountException
        System.out.println("=== DISCOUNT CONCURRENCY STRESS TEST RESULTS ===");
        System.out.println("Total Threads: " + totalThreads);
        System.out.println("Success Count: " + successCount.get());
        System.out.println("Failure Count: " + failureCount.get());

        assertThat(successCount.get()).isEqualTo(expectedSuccess);
        assertThat(failureCount.get()).isEqualTo(expectedFailures);
        assertThat(successCount.get() + failureCount.get()).isEqualTo(totalThreads);

        // Verify discount code state in database
        DiscountCode finalDiscount = discountCodeRepository.findById(testDiscountId)
                .orElseThrow(() -> new IllegalStateException("Discount code not found"));

        System.out.println("Final Discount used_count in DB: " + finalDiscount.getUsedCount());
        assertThat(finalDiscount.getUsedCount()).isEqualTo(expectedSuccess);

        // Verify discount_usage table has exactly 5 records
        long usageCount = discountUsageRepository.countByDiscountDiscountId(testDiscountId);
        System.out.println("Total discount_usage records in DB: " + usageCount);
        assertThat(usageCount).isEqualTo(expectedSuccess);

        // Verify all 5 orders have unique order codes and received the discount
        Set<String> orderCodes = new HashSet<>();
        for (OrderResponse order : createdOrders) {
            assertThat(orderCodes.add(order.getOrderCode()))
                    .as("Order code must be unique: " + order.getOrderCode())
                    .isTrue();
            assertThat(order.getDiscountAmount()).isEqualByComparingTo("100000.00");
            assertThat(order.getDiscountId()).isEqualTo(testDiscountId);
        }
    }
}
