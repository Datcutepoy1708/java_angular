package com.store.service;

import com.store.dto.request.cart.AddToCartRequest;
import com.store.dto.request.cart.CartItemSyncDto;
import com.store.dto.request.cart.MergeCartRequest;
import com.store.dto.response.cart.CartResponse;
import com.store.entity.cart.CartItem;
import com.store.entity.product.Product;
import com.store.entity.product.ProductImage;
import com.store.entity.product.ProductStatus;
import com.store.entity.product.ProductVariant;
import com.store.entity.user.User;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.CartItemRepository;
import com.store.repository.InventoryRepository;
import com.store.repository.ProductVariantRepository;
import com.store.repository.UserRepository;
import com.store.repository.VariantStockSummaryProjection;
import com.store.service.impl.CartServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User mockUser;
    private Product mockProduct;
    private ProductVariant mockVariant1;
    private ProductVariant mockVariant2;
    private CartItem mockCartItem1;
    private CartItem mockCartItem2;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(1L)
                .email("user@store.com")
                .fullName("Test User")
                .build();

        mockProduct = Product.builder()
                .productId(10L)
                .name("MacBook Pro M5")
                .slug("macbook-pro-m5")
                .status(ProductStatus.ACTIVE)
                .images(new ArrayList<>(List.of(
                        ProductImage.builder().imageId(1L).imageUrl("https://example.com/macbook.jpg").sortOrder(0).build()
                )))
                .build();

        mockVariant1 = ProductVariant.builder()
                .variantId(101L)
                .variantName("16GB / 512GB Space Gray")
                .skuVariant("MBP-M5-16-512-SG")
                .price(new BigDecimal("45000000.00"))
                .salePrice(new BigDecimal("42000000.00"))
                .product(mockProduct)
                .build();

        mockVariant2 = ProductVariant.builder()
                .variantId(102L)
                .variantName("32GB / 1TB Silver")
                .skuVariant("MBP-M5-32-1TB-SL")
                .price(new BigDecimal("55000000.00"))
                .salePrice(null)
                .product(mockProduct)
                .build();

        mockCartItem1 = CartItem.builder()
                .cartId(1L)
                .user(mockUser)
                .variant(mockVariant1)
                .quantity(2)
                .createdAt(LocalDateTime.now())
                .build();

        mockCartItem2 = CartItem.builder()
                .cartId(2L)
                .user(mockUser)
                .variant(mockVariant2)
                .quantity(1)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get Cart Tests")
    class GetCartTests {

        @Test
        @DisplayName("Should return empty cart when user has no items")
        void testGetCart_Empty() {
            when(cartItemRepository.findByUserIdWithDetails(1L)).thenReturn(Collections.emptyList());

            CartResponse response = cartService.getCart(1L);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getTotalItems()).isZero();
            assertThat(response.getTotalQuantity()).isZero();
            assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
            assertThat(response.getRemovedStaleItemsCount()).isZero();
        }

        @Test
        @DisplayName("Should return cart with valid items, batch stock query and calculated totals")
        void testGetCart_WithValidItems() {
            when(cartItemRepository.findByUserIdWithDetails(1L)).thenReturn(List.of(mockCartItem1, mockCartItem2));

            VariantStockSummaryProjection proj1 = createMockProjection(101L, 10L);
            VariantStockSummaryProjection proj2 = createMockProjection(102L, 0L);
            when(inventoryRepository.findAvailableStockByVariantIds(anyList())).thenReturn(List.of(proj1, proj2));

            CartResponse response = cartService.getCart(1L);

            assertThat(response).isNotNull();
            assertThat(response.getTotalItems()).isEqualTo(2);
            assertThat(response.getTotalQuantity()).isEqualTo(3); // 2 + 1
            // Item 1: 42,000,000 * 2 = 84,000,000 | Item 2: 55,000,000 * 1 = 55,000,000
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("139000000.00"));
            // Original: (45,000,000 * 2) + (55,000,000 * 1) = 145,000,000
            assertThat(response.getOriginalTotalAmount()).isEqualByComparingTo(new BigDecimal("145000000.00"));
            assertThat(response.getSavingsAmount()).isEqualByComparingTo(new BigDecimal("6000000.00"));

            assertThat(response.getItems().get(0).getAvailableQty()).isEqualTo(10L);
            assertThat(response.getItems().get(0).getIsAvailable()).isTrue();
            assertThat(response.getItems().get(0).getIsExceededStock()).isFalse();

            assertThat(response.getItems().get(1).getAvailableQty()).isEqualTo(0L);
            assertThat(response.getItems().get(1).getIsAvailable()).isFalse();
            assertThat(response.getItems().get(1).getIsExceededStock()).isTrue(); // 1 > 0
        }

        @Test
        @DisplayName("Should auto-clean stale items when variant or product is soft-deleted")
        void testGetCart_CleansStaleDeletedItems() {
            ProductVariant deletedVariant = ProductVariant.builder()
                    .variantId(999L)
                    .variantName("Deleted Variant")
                    .deletedAt(LocalDateTime.now())
                    .product(mockProduct)
                    .build();

            CartItem staleCartItem = CartItem.builder()
                    .cartId(99L)
                    .user(mockUser)
                    .variant(deletedVariant)
                    .quantity(1)
                    .build();

            when(cartItemRepository.findByUserIdWithDetails(1L)).thenReturn(List.of(mockCartItem1, staleCartItem));
            when(inventoryRepository.findAvailableStockByVariantIds(List.of(101L)))
                    .thenReturn(List.of(createMockProjection(101L, 5L)));

            CartResponse response = cartService.getCart(1L);

            verify(cartItemRepository).deleteAll(List.of(staleCartItem));
            assertThat(response.getTotalItems()).isEqualTo(1);
            assertThat(response.getRemovedStaleItemsCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Add to Cart Tests")
    class AddToCartTests {

        @Test
        @DisplayName("Should add new item to cart via atomic upsert successfully")
        void testAddToCart_NewItem() {
            AddToCartRequest request = new AddToCartRequest(101L, 2);

            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
            when(productVariantRepository.findById(101L)).thenReturn(Optional.of(mockVariant1));

            // After add, getCart is called
            when(cartItemRepository.findByUserIdWithDetails(1L)).thenReturn(List.of(mockCartItem1));
            when(inventoryRepository.findAvailableStockByVariantIds(List.of(101L)))
                    .thenReturn(List.of(createMockProjection(101L, 15L)));

            CartResponse response = cartService.addToCart(1L, request);

            verify(cartItemRepository).upsertCartItemAtomic(1L, 101L, 2);
            assertThat(response).isNotNull();
            assertThat(response.getTotalQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should accumulate quantity atomically when item already exists in cart")
        void testAddToCart_ExistingItem_AccumulatesQuantity() {
            AddToCartRequest request = new AddToCartRequest(101L, 3);

            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
            when(productVariantRepository.findById(101L)).thenReturn(Optional.of(mockVariant1));

            when(cartItemRepository.findByUserIdWithDetails(1L)).thenReturn(List.of(
                    CartItem.builder().cartId(1L).user(mockUser).variant(mockVariant1).quantity(5).build()
            ));
            when(inventoryRepository.findAvailableStockByVariantIds(List.of(101L)))
                    .thenReturn(List.of(createMockProjection(101L, 20L)));

            CartResponse response = cartService.addToCart(1L, request);

            verify(cartItemRepository).upsertCartItemAtomic(1L, 101L, 3);
            assertThat(response.getTotalQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when variant is deleted or inactive")
        void testAddToCart_InactiveVariant_ThrowsException() {
            ProductVariant inactiveVariant = ProductVariant.builder()
                    .variantId(103L)
                    .deletedAt(LocalDateTime.now())
                    .product(mockProduct)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
            when(productVariantRepository.findById(103L)).thenReturn(Optional.of(inactiveVariant));

            AddToCartRequest request = new AddToCartRequest(103L, 1);

            assertThatThrownBy(() -> cartService.addToCart(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Biến thể sản phẩm không tồn tại");

            verify(cartItemRepository, never()).upsertCartItemAtomic(any(), any(), org.mockito.ArgumentMatchers.anyInt());
        }
    }

    @Nested
    @DisplayName("Update & Remove Tests")
    class UpdateRemoveTests {

        @Test
        @DisplayName("Should update item quantity successfully")
        void testUpdateQuantity_Success() {
            when(cartItemRepository.findByCartIdAndUserUserId(1L, 1L)).thenReturn(Optional.of(mockCartItem1));
            when(cartItemRepository.findByUserIdWithDetails(1L)).thenReturn(List.of(mockCartItem1));
            when(inventoryRepository.findAvailableStockByVariantIds(List.of(101L)))
                    .thenReturn(List.of(createMockProjection(101L, 10L)));

            CartResponse response = cartService.updateQuantity(1L, 1L, 4);

            assertThat(mockCartItem1.getQuantity()).isEqualTo(4);
            verify(cartItemRepository).save(mockCartItem1);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should delete cart item when quantity is 0 or negative")
        void testUpdateQuantity_Zero_DeletesItem() {
            when(cartItemRepository.findByCartIdAndUserUserId(1L, 1L)).thenReturn(Optional.of(mockCartItem1));
            when(cartItemRepository.findByUserIdWithDetails(1L)).thenReturn(Collections.emptyList());

            CartResponse response = cartService.updateQuantity(1L, 1L, 0);

            verify(cartItemRepository).delete(mockCartItem1);
            assertThat(response.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Should remove cart item successfully")
        void testRemoveItem_Success() {
            when(cartItemRepository.findByCartIdAndUserUserId(1L, 1L)).thenReturn(Optional.of(mockCartItem1));
            when(cartItemRepository.findByUserIdWithDetails(1L)).thenReturn(Collections.emptyList());

            CartResponse response = cartService.removeItem(1L, 1L);

            verify(cartItemRepository).delete(mockCartItem1);
            assertThat(response.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Should clear entire cart")
        void testClearCart_Success() {
            cartService.clearCart(1L);
            verify(cartItemRepository).deleteByUserUserId(1L);
        }
    }

    @Nested
    @DisplayName("Merge Guest Cart Tests")
    class MergeGuestCartTests {

        @Test
        @DisplayName("Should accumulate quantities for existing items and ignore stale guest variants")
        void testMergeGuestCart_Success() {
            MergeCartRequest request = MergeCartRequest.builder()
                    .items(List.of(
                            new CartItemSyncDto(101L, 3), // Valid
                            new CartItemSyncDto(102L, 2), // Valid
                            new CartItemSyncDto(999L, 1)  // Stale/Non-existent (should be ignored)
                    ))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
            when(productVariantRepository.findById(101L)).thenReturn(Optional.of(mockVariant1));
            when(productVariantRepository.findById(102L)).thenReturn(Optional.of(mockVariant2));
            when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

            when(cartItemRepository.findByUserIdWithDetails(1L)).thenReturn(List.of(
                    CartItem.builder().cartId(1L).user(mockUser).variant(mockVariant1).quantity(5).build(),
                    CartItem.builder().cartId(2L).user(mockUser).variant(mockVariant2).quantity(2).build()
            ));
            when(inventoryRepository.findAvailableStockByVariantIds(anyList()))
                    .thenReturn(List.of(createMockProjection(101L, 10L), createMockProjection(102L, 5L)));

            CartResponse response = cartService.mergeGuestCart(1L, request);

            verify(cartItemRepository).upsertCartItemAtomic(1L, 101L, 3);
            verify(cartItemRepository).upsertCartItemAtomic(1L, 102L, 2);
            assertThat(response.getTotalQuantity()).isEqualTo(7); // 5 + 2
            assertThat(response.getRemovedStaleItemsCount()).isEqualTo(1); // Item 999 ignored
        }
    }

    private VariantStockSummaryProjection createMockProjection(Long variantId, Long availableQty) {
        return new VariantStockSummaryProjection() {
            @Override
            public Long getVariantId() {
                return variantId;
            }

            @Override
            public Long getAvailableQty() {
                return availableQty;
            }
        };
    }
}
