package com.store.service;

import com.store.dto.returnrefund.*;
import com.store.entity.discount.DiscountCode;
import com.store.entity.order.Order;
import com.store.entity.order.OrderItem;
import com.store.entity.order.OrderStatus;
import com.store.entity.product.Product;
import com.store.entity.product.ProductVariant;
import com.store.entity.returnrefund.*;
import com.store.entity.user.User;
import com.store.exception.BadRequestException;
import com.store.repository.*;
import com.store.service.impl.ReturnServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private ReturnRequestItemRepository returnRequestItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DiscountCodeRepository discountCodeRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private SettingService settingService;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private ReturnServiceImpl returnService;

    private User testUser;
    private Order testOrder;
    private OrderItem testOrderItem;
    private ProductVariant testVariant;
    private DiscountCode testDiscount;
    private ReturnRequest sampleReturnRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(10L)
                .email("customer@store.com")
                .fullName("Nguyễn Văn A")
                .phone("0912345678")
                .build();

        Product product = Product.builder().productId(1L).name("Laptop Gaming Pro").build();
        testVariant = ProductVariant.builder().variantId(100L).product(product).variantName("16GB / 512GB").skuVariant("LGP-16-512").build();

        testOrderItem = OrderItem.builder()
                .orderItemId(50L)
                .variant(testVariant)
                .productNameSnapshot("Laptop Gaming Pro")
                .priceSnapshot(new BigDecimal("25000000"))
                .quantity(1)
                .subtotal(new BigDecimal("25000000"))
                .build();

        testOrder = Order.builder()
                .orderId(1001L)
                .orderCode("ORD-202608-1001")
                .user(testUser)
                .orderStatus(OrderStatus.COMPLETED)
                .totalAmount(new BigDecimal("25000000"))
                .discountId(5L)
                .items(new ArrayList<>(List.of(testOrderItem)))
                .createdAt(LocalDateTime.now().minusDays(3))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        ReturnRequestItem rri = ReturnRequestItem.builder()
                .id(1L)
                .orderItem(testOrderItem)
                .variant(testVariant)
                .quantity(1)
                .unitPrice(new BigDecimal("25000000"))
                .itemCondition(ItemCondition.OPENED)
                .build();

        sampleReturnRequest = ReturnRequest.builder()
                .returnId(1L)
                .returnCode("RET-20260826-0001")
                .order(testOrder)
                .user(testUser)
                .status(ReturnStatus.REQUESTED)
                .returnReason(ReturnReason.DEFECTIVE)
                .customerNote("Máy bị lỗi màn hình sọc xanh")
                .refundAmount(new BigDecimal("25000000"))
                .restockWarehouseId(1)
                .items(new ArrayList<>(List.of(rri)))
                .requestedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createReturnRequest should succeed for valid completed order within return window")
    void createReturnRequest_Success() {
        ReturnCreateRequest request = ReturnCreateRequest.builder()
                .orderId(1001L)
                .returnReason("DEFECTIVE")
                .customerNote("Lỗi phần cứng")
                .items(List.of(
                        ReturnCreateRequest.ReturnItemRequest.builder()
                                .orderItemId(50L)
                                .quantity(1)
                                .itemCondition("OPENED")
                                .build()
                ))
                .bankName("Vietcombank")
                .bankAccountNumber("1234567890")
                .bankAccountName("NGUYEN VAN A")
                .build();

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(testOrder));
        when(settingService.getReturnWindowDays()).thenReturn(14);
        when(returnRequestRepository.existsByOrderOrderIdAndStatusNotIn(eq(1001L), any())).thenReturn(false);
        when(returnRequestRepository.countByReturnCodePrefix(anyString())).thenReturn(0L);
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> {
            ReturnRequest r = i.getArgument(0);
            r.setReturnId(1L);
            return r;
        });

        ReturnDetailResponse response = returnService.createReturnRequest(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getReturnCode()).contains("RET-");
        assertThat(response.getRefundAmount()).isEqualByComparingTo(new BigDecimal("25000000"));
        assertThat(response.getStatus()).isEqualTo("REQUESTED");
        verify(returnRequestRepository).save(any(ReturnRequest.class));
    }

    @Test
    @DisplayName("createReturnRequest should throw BadRequestException if order status is not COMPLETED")
    void createReturnRequest_OrderNotCompleted_ThrowsException() {
        testOrder.setOrderStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(testOrder));

        ReturnCreateRequest request = ReturnCreateRequest.builder()
                .orderId(1001L)
                .returnReason("DEFECTIVE")
                .items(List.of(ReturnCreateRequest.ReturnItemRequest.builder().orderItemId(50L).quantity(1).build()))
                .build();

        assertThatThrownBy(() -> returnService.createReturnRequest(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    @DisplayName("createReturnRequest should throw BadRequestException if beyond allowed return window")
    void createReturnRequest_BeyondReturnWindow_ThrowsException() {
        testOrder.setUpdatedAt(LocalDateTime.now().minusDays(20));
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(testOrder));
        when(settingService.getReturnWindowDays()).thenReturn(14);

        ReturnCreateRequest request = ReturnCreateRequest.builder()
                .orderId(1001L)
                .returnReason("DEFECTIVE")
                .items(List.of(ReturnCreateRequest.ReturnItemRequest.builder().orderItemId(50L).quantity(1).build()))
                .build();

        assertThatThrownBy(() -> returnService.createReturnRequest(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("vượt quá thời hạn");
    }

    @Test
    @DisplayName("reviewReturnRequest should approve or reject return request")
    void reviewReturnRequest_Approve_Success() {
        when(returnRequestRepository.findById(1L)).thenReturn(Optional.of(sampleReturnRequest));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReturnReviewRequest reviewReq = ReturnReviewRequest.builder()
                .approved(true)
                .adminNote("Chấp thuận cho gửi hàng về kho kiểm tra")
                .build();

        ReturnDetailResponse response = returnService.reviewReturnRequest(1L, 1L, reviewReq);

        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("receiveReturnedItems should update status to ITEM_RECEIVED and update condition")
    void receiveReturnedItems_Success() {
        sampleReturnRequest.setStatus(ReturnStatus.APPROVED);
        when(returnRequestRepository.findById(1L)).thenReturn(Optional.of(sampleReturnRequest));
        when(warehouseRepository.existsById(1)).thenReturn(true);
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(com.store.entity.inventory.Warehouse.builder().warehouseId(1).name("Kho Tổng").build()));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReturnReceiveItemRequest req = ReturnReceiveItemRequest.builder()
                .warehouseId(1)
                .adminNote("Đã nhận máy tại kho")
                .itemConditions(List.of(ReturnReceiveItemRequest.ItemConditionUpdate.builder().returnItemId(1L).condition("OPENED").build()))
                .build();

        ReturnDetailResponse response = returnService.receiveReturnedItems(1L, 1L, req);

        assertThat(response.getStatus()).isEqualTo("ITEM_RECEIVED");
        assertThat(response.getReceivedAt()).isNotNull();
    }

    @Test
    @DisplayName("processRefund should restock atomic, decrement discount used count on full return, and complete refund")
    void processRefund_FullReturn_RestocksAndDecrementsDiscount() {
        sampleReturnRequest.setStatus(ReturnStatus.ITEM_RECEIVED);
        sampleReturnRequest.setRestockWarehouseId(1);
        when(returnRequestRepository.findById(1L)).thenReturn(Optional.of(sampleReturnRequest));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReturnProcessRefundRequest refundReq = ReturnProcessRefundRequest.builder()
                .refundTransactionCode("FT2608269988")
                .adminNote("Đã chuyển khoản hoàn tiền thành công")
                .build();

        ReturnDetailResponse response = returnService.processRefund(1L, 1L, refundReq);

        assertThat(response.getStatus()).isEqualTo("REFUNDED");
        assertThat(response.getRefundTransactionCode()).isEqualTo("FT2608269988");

        // Verify Atomic Restock called with condition
        verify(inventoryService).restockReturnedItemAtomic(
                eq(100L), eq(1), eq(1), eq("OPENED"), eq(1L), eq("RET-20260826-0001"), eq(1L)
        );

        // Verify discount usage count decremented for Full Order Return
        verify(discountCodeRepository).decrementUsedCountAtomic(5L);

        // Verify order status cancelled
        verify(orderRepository).save(argThat(o -> o.getOrderStatus() == OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("processRefund should keep discount usage count and order status unchanged on partial return")
    void processRefund_PartialReturn_KeepsDiscountAndOrderStatus() {
        // Setup order with 2 items
        OrderItem secondItem = OrderItem.builder()
                .orderItemId(51L)
                .variant(testVariant)
                .productNameSnapshot("Chuột Gaming")
                .priceSnapshot(new BigDecimal("1000000"))
                .quantity(1)
                .subtotal(new BigDecimal("1000000"))
                .build();
        testOrder.getItems().add(secondItem);

        // Return request only contains the first item (Partial return)
        sampleReturnRequest.setStatus(ReturnStatus.ITEM_RECEIVED);
        sampleReturnRequest.setRestockWarehouseId(1);
        when(returnRequestRepository.findById(1L)).thenReturn(Optional.of(sampleReturnRequest));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReturnProcessRefundRequest refundReq = ReturnProcessRefundRequest.builder()
                .refundTransactionCode("FT2608269999")
                .adminNote("Hoàn tiền 1 phần đơn hàng")
                .build();

        ReturnDetailResponse response = returnService.processRefund(1L, 1L, refundReq);

        assertThat(response.getStatus()).isEqualTo("REFUNDED");

        // Verify Atomic Restock still called for the returned item
        verify(inventoryService).restockReturnedItemAtomic(
                eq(100L), eq(1), eq(1), eq("OPENED"), eq(1L), eq("RET-20260826-0001"), eq(1L)
        );

        // Verify discount usage count is NEVER decremented on partial return
        verify(discountCodeRepository, never()).decrementUsedCountAtomic(anyLong());

        // Verify order status is NOT changed to CANCELLED
        verify(orderRepository, never()).save(argThat(o -> o.getOrderStatus() == OrderStatus.CANCELLED));
        assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
}
