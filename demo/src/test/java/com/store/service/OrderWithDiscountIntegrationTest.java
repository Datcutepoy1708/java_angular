package com.store.service;

import com.store.dto.request.cart.AddToCartRequest;
import com.store.dto.request.order.CreateOrderRequest;
import com.store.dto.response.order.OrderResponse;
import com.store.entity.discount.DiscountCode;
import com.store.entity.discount.DiscountStatus;
import com.store.entity.discount.DiscountType;
import com.store.entity.inventory.Inventory;
import com.store.entity.inventory.Warehouse;
import com.store.entity.order.OrderStatus;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
class OrderWithDiscountIntegrationTest {

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

    private Long testVariantId;
    private Integer testWarehouseId;
    private Long testUserId;
    private Long testDiscountId;
    private String testCode;

    @BeforeEach
    void setUp() {
        ProductVariant variant = productVariantRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No variants found for test"));
        Warehouse warehouse = warehouseRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No warehouses found for test"));

        testVariantId = variant.getVariantId();
        testWarehouseId = warehouse.getWarehouseId();

        // Setup clean inventory
        List<Inventory> inventories = inventoryRepository.findAll().stream()
                .filter(inv -> inv.getVariant().getVariantId().equals(testVariantId))
                .toList();

        for (Inventory inv : inventories) {
            inv.setQuantity(0);
            inv.setReservedQty(0);
            inventoryRepository.save(inv);
        }

        Inventory targetInv = inventories.stream()
                .filter(inv -> inv.getWarehouse().getWarehouseId().equals(testWarehouseId))
                .findFirst()
                .orElseGet(() -> Inventory.builder().variant(variant).warehouse(warehouse).quantity(0).reservedQty(0).build());

        targetInv.setQuantity(10);
        targetInv.setReservedQty(0);
        inventoryRepository.save(targetInv);

        // Setup user
        String email = "discount_test_user@test.com";
        User user = userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(email)
                        .passwordHash("$2a$10$dummyHashForTestingPurposesOnly1234567890")
                        .fullName("Discount Tester")
                        .phone("0987111222")
                        .build())
        );
        testUserId = user.getUserId();

        // Create unique coupon per test run
        testCode = "VOUCHER_" + System.nanoTime();
        DiscountCode discount = DiscountCode.builder()
                .code(testCode)
                .description("Giảm 100.000đ cho đơn hàng")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("100000.00"))
                .minOrderValue(BigDecimal.ZERO)
                .usageLimit(10)
                .usageLimitPerUser(1)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .status(DiscountStatus.ACTIVE)
                .build();
        DiscountCode saved = discountCodeRepository.save(discount);
        testDiscountId = saved.getDiscountId();
    }

    @AfterEach
    void tearDown() {
        cartService.clearCart(testUserId);
    }

    @Test
    @DisplayName("End-to-End: Create order with discount -> used_count=1, discount_usage record created, total amount reduced")
    void testCreateOrderWithDiscount_Success() {
        cartService.clearCart(testUserId);
        cartService.addToCart(testUserId, AddToCartRequest.builder()
                .variantId(testVariantId)
                .quantity(1)
                .build());

        CreateOrderRequest req = CreateOrderRequest.builder()
                .receiverName("Discount Tester")
                .receiverPhone("0987111222")
                .shippingAddress("123 Test Street, Ha Noi")
                .paymentMethod(PaymentMethod.COD)
                .discountCode(testCode)
                .build();

        OrderResponse response = orderService.createOrder(testUserId, req);

        assertThat(response).isNotNull();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("100000.00");
        assertThat(response.getDiscountId()).isEqualTo(testDiscountId);
        assertThat(response.getDiscountCode()).isEqualTo(testCode);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(response.getSubtotal().subtract(new BigDecimal("100000.00")));

        // Check DB state
        DiscountCode inDb = discountCodeRepository.findById(testDiscountId).orElseThrow();
        assertThat(inDb.getUsedCount()).isEqualTo(1);

        long usageCount = discountUsageRepository.countByDiscountDiscountIdAndUserUserId(testDiscountId, testUserId);
        assertThat(usageCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Cancellation Rollback: Cancelling order with discount -> used_count returns to 0, discount_usage is deleted, user can reuse code")
    void testCancelOrderWithDiscount_RollsBackUsedCountAndUsageLog() {
        // Step 1: Place order with discount
        cartService.clearCart(testUserId);
        cartService.addToCart(testUserId, AddToCartRequest.builder()
                .variantId(testVariantId)
                .quantity(1)
                .build());

        CreateOrderRequest req = CreateOrderRequest.builder()
                .receiverName("Discount Tester")
                .receiverPhone("0987111222")
                .shippingAddress("123 Test Street, Ha Noi")
                .paymentMethod(PaymentMethod.COD)
                .discountCode(testCode)
                .build();

        OrderResponse order = orderService.createOrder(testUserId, req);
        assertThat(discountCodeRepository.findById(testDiscountId).orElseThrow().getUsedCount()).isEqualTo(1);

        // Step 2: Cancel order
        orderService.cancelOrderByCustomer(order.getOrderCode(), testUserId, "Đổi ý không mua nữa");

        // Verify discount state is fully restored
        DiscountCode restored = discountCodeRepository.findById(testDiscountId).orElseThrow();
        assertThat(restored.getUsedCount()).isEqualTo(0);

        long usageRecords = discountUsageRepository.countByDiscountDiscountIdAndUserUserId(testDiscountId, testUserId);
        assertThat(usageRecords).isEqualTo(0);

        // Step 3: User can place a NEW order reusing the same discount code successfully
        cartService.addToCart(testUserId, AddToCartRequest.builder()
                .variantId(testVariantId)
                .quantity(1)
                .build());

        OrderResponse secondOrder = orderService.createOrder(testUserId, req);
        assertThat(secondOrder).isNotNull();
        assertThat(secondOrder.getDiscountAmount()).isEqualByComparingTo("100000.00");
        assertThat(discountCodeRepository.findById(testDiscountId).orElseThrow().getUsedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("DB Rollback Test: When discount is exhausted at Step 4, reserved inventory stock rolls back to 0, 0 orders saved, and user cart is intact")
    void testCreateOrder_DiscountExhausted_RollsBackReservedStock() {
        // Artificially set used_count to usage_limit
        DiscountCode discount = discountCodeRepository.findById(testDiscountId).orElseThrow();
        discount.setUsedCount(discount.getUsageLimit()); // Exhausted
        discountCodeRepository.save(discount);

        cartService.clearCart(testUserId);
        cartService.addToCart(testUserId, AddToCartRequest.builder()
                .variantId(testVariantId)
                .quantity(2)
                .build());

        long initialOrderCount = orderRepository.count();

        CreateOrderRequest req = CreateOrderRequest.builder()
                .receiverName("Discount Tester")
                .receiverPhone("0987111222")
                .shippingAddress("123 Test Street, Ha Noi")
                .paymentMethod(PaymentMethod.COD)
                .discountCode(testCode)
                .build();

        assertThatThrownBy(() -> orderService.createOrder(testUserId, req))
                .isInstanceOf(InvalidDiscountException.class);

        // Condition 1: Reserved inventory stock must be rolled back to 0
        Inventory inv = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(testVariantId, testWarehouseId).orElseThrow();
        assertThat(inv.getReservedQty()).isEqualTo(0);
        assertThat(inv.getQuantity()).isEqualTo(10);

        // Condition 2: No order created in database
        assertThat(orderRepository.count()).isEqualTo(initialOrderCount);

        // Condition 3: User cart remains intact
        List<?> remainingCart = cartItemRepository.findByUserIdWithDetails(testUserId);
        assertThat(remainingCart).hasSize(1);
    }
}
