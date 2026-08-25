package com.store.service;

import com.store.dto.request.order.CreateOrderRequest;
import com.store.dto.request.order.UpdateOrderStatusRequest;
import com.store.dto.request.order.UpdatePaymentStatusRequest;
import com.store.dto.response.order.OrderResponse;
import com.store.entity.cart.CartItem;
import com.store.entity.inventory.Inventory;
import com.store.entity.inventory.Warehouse;
import com.store.entity.order.Address;
import com.store.entity.order.Order;
import com.store.entity.order.OrderItem;
import com.store.entity.order.OrderStatus;
import com.store.entity.order.OrderStatusHistory;
import com.store.entity.order.PaymentMethod;
import com.store.entity.order.PaymentStatus;
import com.store.entity.product.Product;
import com.store.entity.product.ProductStatus;
import com.store.entity.product.ProductVariant;
import com.store.entity.user.User;
import com.store.exception.InsufficientStockException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.AddressRepository;
import com.store.repository.CartItemRepository;
import com.store.repository.InventoryRepository;
import com.store.repository.OrderItemRepository;
import com.store.repository.OrderRepository;
import com.store.repository.OrderStatusHistoryRepository;
import com.store.repository.ProductImageRepository;
import com.store.repository.UserRepository;
import com.store.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Warehouse warehouseHanoi;
    private Product testProduct1;
    private ProductVariant testVariant1;
    private Product testProduct2;
    private ProductVariant testVariant2;
    private Product testProduct3;
    private ProductVariant testVariant3;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .fullName("Nguyen Van A")
                .email("vana@example.com")
                .phone("0901234567")
                .build();

        warehouseHanoi = Warehouse.builder()
                .warehouseId(1)
                .name("Kho Tổng Hà Nội")
                .build();

        testProduct1 = Product.builder()
                .productId(10L)
                .name("Laptop ASUS ROG Strix")
                .status(ProductStatus.ACTIVE)
                .build();

        testVariant1 = ProductVariant.builder()
                .variantId(101L)
                .product(testProduct1)
                .variantName("i7 / 16GB / 512GB")
                .price(BigDecimal.valueOf(25000000))
                .build();

        testProduct2 = Product.builder()
                .productId(20L)
                .name("Chuột Logitech G502")
                .status(ProductStatus.ACTIVE)
                .build();

        testVariant2 = ProductVariant.builder()
                .variantId(102L)
                .product(testProduct2)
                .variantName("Đen RGB")
                .price(BigDecimal.valueOf(1200000))
                .build();

        testProduct3 = Product.builder()
                .productId(30L)
                .name("Bàn phím cơ AKKO 3087")
                .status(ProductStatus.ACTIVE)
                .build();

        testVariant3 = ProductVariant.builder()
                .variantId(103L)
                .product(testProduct3)
                .variantName("Pink Switch")
                .price(BigDecimal.valueOf(1500000))
                .build();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should successfully place order, hold stock atomically from priority warehouse, and clear user cart")
        void testCreateOrder_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            CartItem cartItem1 = CartItem.builder().cartId(1L).user(testUser).variant(testVariant1).quantity(1).build();
            CartItem cartItem2 = CartItem.builder().cartId(2L).user(testUser).variant(testVariant2).quantity(2).build();
            when(cartItemRepository.findByUserIdWithDetails(1L)).thenReturn(List.of(cartItem1, cartItem2));

            Inventory inv1 = Inventory.builder().variant(testVariant1).warehouse(warehouseHanoi).quantity(10).reservedQty(0).build();
            Inventory inv2 = Inventory.builder().variant(testVariant2).warehouse(warehouseHanoi).quantity(10).reservedQty(0).build();

            when(inventoryRepository.findWarehousesWithAvailableStock(101L, 1)).thenReturn(List.of(inv1));
            when(inventoryRepository.findWarehousesWithAvailableStock(102L, 2)).thenReturn(List.of(inv2));

            when(inventoryRepository.reserveStockAtomic(101L, 1, 1)).thenReturn(1);
            when(inventoryRepository.reserveStockAtomic(102L, 1, 2)).thenReturn(1);

            when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setOrderId(100L);
                order.setCreatedAt(LocalDateTime.now());
                return order;
            });

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .receiverName("Nguyen Van A")
                    .receiverPhone("0901234567")
                    .shippingAddress("123 Đường Cầu Giấy, Hà Nội")
                    .paymentMethod(PaymentMethod.COD)
                    .note("Giao giờ hành chính")
                    .build();

            OrderResponse response = orderService.createOrder(1L, request);

            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(100L);
            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
            assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
            assertThat(response.getItems()).hasSize(2);
            // 25,000,000 * 1 + 1,200,000 * 2 = 27,400,000
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(27400000));

            // Verify stock holds were executed
            verify(inventoryRepository).reserveStockAtomic(101L, 1, 1);
            verify(inventoryRepository).reserveStockAtomic(102L, 1, 2);

            // Verify cart cleared
            verify(cartItemRepository).deleteByUserUserId(1L);
        }

        @Test
        @DisplayName("Should throw IllegalStateException if cart is empty")
        void testCreateOrder_EmptyCart_ThrowsIllegalStateException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(cartItemRepository.findByUserIdWithDetails(1L)).thenReturn(Collections.emptyList());

            CreateOrderRequest request = CreateOrderRequest.builder().shippingAddress("Hà Nội").build();

            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Giỏ hàng của bạn đang trống");

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should fail and rollback when item 3 has insufficient stock, even if item 1 and 2 had stock")
        void testCreateOrder_InsufficientStock_RollsBackAllItems() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            CartItem cartItem1 = CartItem.builder().cartId(1L).user(testUser).variant(testVariant1).quantity(1).build();
            CartItem cartItem2 = CartItem.builder().cartId(2L).user(testUser).variant(testVariant2).quantity(1).build();
            CartItem cartItem3 = CartItem.builder().cartId(3L).user(testUser).variant(testVariant3).quantity(5).build();
            when(cartItemRepository.findByUserIdWithDetails(1L)).thenReturn(List.of(cartItem1, cartItem2, cartItem3));

            Inventory inv1 = Inventory.builder().variant(testVariant1).warehouse(warehouseHanoi).quantity(10).reservedQty(0).build();
            Inventory inv2 = Inventory.builder().variant(testVariant2).warehouse(warehouseHanoi).quantity(10).reservedQty(0).build();

            when(inventoryRepository.findWarehousesWithAvailableStock(101L, 1)).thenReturn(List.of(inv1));
            when(inventoryRepository.findWarehousesWithAvailableStock(102L, 1)).thenReturn(List.of(inv2));
            // Variant 3 has 0 available in any warehouse
            when(inventoryRepository.findWarehousesWithAvailableStock(103L, 5)).thenReturn(Collections.emptyList());

            when(inventoryRepository.reserveStockAtomic(101L, 1, 1)).thenReturn(1);
            when(inventoryRepository.reserveStockAtomic(102L, 1, 1)).thenReturn(1);

            CreateOrderRequest request = CreateOrderRequest.builder().shippingAddress("Hà Nội").build();

            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("không đủ tồn kho");

            // Order must NOT be saved, cart must NOT be deleted, and releaseStockAtomic is not called manually (DB rollback handles it)
            verify(orderRepository, never()).save(any());
            verify(cartItemRepository, never()).deleteByUserUserId(anyLong());
            verify(inventoryRepository, never()).releaseStockAtomic(anyLong(), anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Customer can successfully cancel PENDING order and releases held stock")
        void testCancelOrderByCustomer_Pending_Success() {
            Order order = Order.builder()
                    .orderId(100L)
                    .orderCode("ORD-20260825-100000-TEST")
                    .user(testUser)
                    .orderStatus(OrderStatus.PENDING)
                    .paymentStatus(PaymentStatus.UNPAID)
                    .totalAmount(BigDecimal.valueOf(25000000))
                    .items(new ArrayList<>())
                    .statusHistory(new ArrayList<>())
                    .build();

            OrderItem item = OrderItem.builder()
                    .variant(testVariant1)
                    .warehouse(warehouseHanoi)
                    .quantity(2)
                    .productNameSnapshot("Laptop ASUS ROG Strix")
                    .priceSnapshot(BigDecimal.valueOf(12500000))
                    .subtotal(BigDecimal.valueOf(25000000))
                    .build();
            order.addItem(item);

            when(orderRepository.findByOrderCodeAndUserUserId("ORD-20260825-100000-TEST", 1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            OrderResponse response = orderService.cancelOrderByCustomer("ORD-20260825-100000-TEST", 1L, "Đổi ý không mua nữa");

            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(inventoryRepository).releaseStockAtomic(101L, 1, 2);
        }

        @Test
        @DisplayName("Customer cannot self-cancel order once it moves to CONFIRMED status")
        void testCancelOrderByCustomer_Confirmed_ThrowsIllegalStateException() {
            Order order = Order.builder()
                    .orderId(100L)
                    .orderCode("ORD-20260825-100000-TEST")
                    .user(testUser)
                    .orderStatus(OrderStatus.CONFIRMED)
                    .build();

            when(orderRepository.findByOrderCodeAndUserUserId("ORD-20260825-100000-TEST", 1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrderByCustomer("ORD-20260825-100000-TEST", 1L, "Hủy giúp tôi"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Chỉ có thể tự hủy đơn hàng khi đang ở trạng thái Chờ xác nhận");

            verify(inventoryRepository, never()).releaseStockAtomic(anyLong(), anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("Admin Order Management Tests")
    class AdminOrderManagementTests {

        @Test
        @DisplayName("Admin completing order permanently deducts physical stock and marks COD as PAID")
        void testCompleteOrderByAdmin_DeductsStockAndMarksCodPaid() {
            Order order = Order.builder()
                    .orderId(100L)
                    .orderCode("ORD-20260825-100000-TEST")
                    .user(testUser)
                    .orderStatus(OrderStatus.SHIPPING)
                    .paymentMethod(PaymentMethod.COD)
                    .paymentStatus(PaymentStatus.UNPAID)
                    .items(new ArrayList<>())
                    .statusHistory(new ArrayList<>())
                    .build();

            OrderItem item = OrderItem.builder()
                    .variant(testVariant1)
                    .warehouse(warehouseHanoi)
                    .quantity(1)
                    .build();
            order.addItem(item);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.COMPLETED)
                    .note("Giao hàng thành công cho khách")
                    .build();

            OrderResponse response = orderService.updateOrderStatusByAdmin(100L, request, 99L);

            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            verify(inventoryRepository).deductCompletedStockAtomic(101L, 1, 1);
        }

        @Test
        @DisplayName("Admin cancelling order releases reserved stock")
        void testCancelOrderByAdmin_ReleasesStock() {
            Order order = Order.builder()
                    .orderId(100L)
                    .orderCode("ORD-20260825-100000-TEST")
                    .user(testUser)
                    .orderStatus(OrderStatus.CONFIRMED)
                    .items(new ArrayList<>())
                    .statusHistory(new ArrayList<>())
                    .build();

            OrderItem item = OrderItem.builder()
                    .variant(testVariant1)
                    .warehouse(warehouseHanoi)
                    .quantity(1)
                    .build();
            order.addItem(item);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.CANCELLED)
                    .note("Khách báo bận không nhận hàng được")
                    .build();

            OrderResponse response = orderService.updateOrderStatusByAdmin(100L, request, 99L);

            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(inventoryRepository).releaseStockAtomic(101L, 1, 1);
        }

        @Test
        @DisplayName("Cannot modify status of an already COMPLETED order")
        void testUpdateOrderStatus_AlreadyCompleted_ThrowsIllegalStateException() {
            Order order = Order.builder()
                    .orderId(100L)
                    .orderStatus(OrderStatus.COMPLETED)
                    .build();

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.CANCELLED)
                    .build();

            assertThatThrownBy(() -> orderService.updateOrderStatusByAdmin(100L, request, 99L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Không thể thay đổi trạng thái của đơn hàng đã Hoàn tất hoặc đã Hủy");
        }

        @Test
        @DisplayName("Admin can update payment status for bank transfer")
        void testUpdatePaymentStatusByAdmin_Success() {
            Order order = Order.builder()
                    .orderId(100L)
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .paymentStatus(PaymentStatus.UNPAID)
                    .orderStatus(OrderStatus.PENDING)
                    .statusHistory(new ArrayList<>())
                    .build();

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            UpdatePaymentStatusRequest request = UpdatePaymentStatusRequest.builder()
                    .paymentStatus(PaymentStatus.PAID)
                    .note("Đã nhận được tiền qua tài khoản Vietcombank")
                    .build();

            OrderResponse response = orderService.updatePaymentStatusByAdmin(100L, request, 99L);

            assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        }
    }

    @Nested
    @DisplayName("Order Timeout Cleanup Tests")
    class OrderTimeoutTests {

        @Test
        @DisplayName("Should scan expired PENDING orders (>24h), cancel them, and release held inventory")
        void testProcessExpiredPendingOrders_CancelsAndReleasesStock() {
            Order expiredOrder = Order.builder()
                    .orderId(100L)
                    .orderCode("ORD-20260824-000000-EXPD")
                    .user(testUser)
                    .orderStatus(OrderStatus.PENDING)
                    .items(new ArrayList<>())
                    .statusHistory(new ArrayList<>())
                    .build();

            OrderItem item = OrderItem.builder()
                    .variant(testVariant1)
                    .warehouse(warehouseHanoi)
                    .quantity(3)
                    .build();
            expiredOrder.addItem(item);

            when(orderRepository.findByOrderStatusAndCreatedAtBefore(eq(OrderStatus.PENDING), any(LocalDateTime.class)))
                    .thenReturn(List.of(expiredOrder));

            int cancelledCount = orderService.processExpiredPendingOrders();

            assertThat(cancelledCount).isEqualTo(1);
            assertThat(expiredOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(inventoryRepository).releaseStockAtomic(101L, 1, 3);
            verify(orderRepository).save(expiredOrder);
        }
    }
}
