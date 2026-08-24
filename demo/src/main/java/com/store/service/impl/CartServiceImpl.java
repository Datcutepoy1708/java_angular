package com.store.service.impl;

import com.store.dto.request.cart.AddToCartRequest;
import com.store.dto.request.cart.CartItemSyncDto;
import com.store.dto.request.cart.MergeCartRequest;
import com.store.dto.response.cart.CartItemResponse;
import com.store.dto.response.cart.CartResponse;
import com.store.entity.cart.CartItem;
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
import com.store.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public CartResponse getCart(Long userId) {
        log.info("Fetching cart for user id: {}", userId);
        List<CartItem> allItems = cartItemRepository.findByUserIdWithDetails(userId);

        List<CartItem> validItems = new ArrayList<>();
        List<CartItem> staleItems = new ArrayList<>();

        for (CartItem item : allItems) {
            ProductVariant variant = item.getVariant();
            if (isVariantValid(variant)) {
                validItems.add(item);
            } else {
                staleItems.add(item);
            }
        }

        // Clean up stale items from DB (soft-deleted variants/products)
        if (!staleItems.isEmpty()) {
            log.info("Cleaning up {} stale cart items for user id: {}", staleItems.size(), userId);
            cartItemRepository.deleteAll(staleItems);
        }

        return buildCartResponse(validItems, staleItems.size());
    }

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        log.info("User {} adding variant {} with qty {} to cart", userId, request.getVariantId(), request.getQuantity());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Biến thể sản phẩm không tồn tại"));

        if (!isVariantValid(variant)) {
            throw new ResourceNotFoundException("Biến thể sản phẩm không tồn tại hoặc đã ngừng kinh doanh");
        }

        // 100% Atomic Upsert: MySQL INSERT ... ON DUPLICATE KEY UPDATE quantity = quantity + :qty
        // Eliminates lost updates, DataIntegrityViolationException and race conditions on uq_user_variant
        cartItemRepository.upsertCartItemAtomic(userId, request.getVariantId(), request.getQuantity());

        return getCart(userId);
    }

    @Override
    @Transactional
    public CartResponse updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        log.info("User {} updating cart item {} with qty {}", userId, cartItemId, quantity);

        CartItem cartItem = cartItemRepository.findByCartIdAndUserUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Mặt hàng không tồn tại trong giỏ"));

        if (quantity == null || quantity <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }

        return getCart(userId);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {
        log.info("User {} removing cart item {}", userId, cartItemId);

        CartItem cartItem = cartItemRepository.findByCartIdAndUserUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Mặt hàng không tồn tại trong giỏ"));

        cartItemRepository.delete(cartItem);
        return getCart(userId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.info("User {} clearing entire cart", userId);
        cartItemRepository.deleteByUserUserId(userId);
    }

    @Override
    @Transactional
    public CartResponse mergeGuestCart(Long userId, MergeCartRequest request) {
        log.info("User {} merging guest cart with {} items", userId, request.getItems() != null ? request.getItems().size() : 0);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        int staleGuestItemsCount = 0;

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CartItemSyncDto syncDto : request.getItems()) {
                if (syncDto.getVariantId() == null || syncDto.getQuantity() == null || syncDto.getQuantity() <= 0) {
                    continue;
                }

                ProductVariant variant = productVariantRepository.findById(syncDto.getVariantId()).orElse(null);
                if (variant != null && isVariantValid(variant)) {
                    cartItemRepository.upsertCartItemAtomic(userId, syncDto.getVariantId(), syncDto.getQuantity());
                } else {
                    staleGuestItemsCount++;
                }
            }
        }

        CartResponse response = getCart(userId);
        if (staleGuestItemsCount > 0) {
            int currentRemoved = response.getRemovedStaleItemsCount() != null ? response.getRemovedStaleItemsCount() : 0;
            response.setRemovedStaleItemsCount(currentRemoved + staleGuestItemsCount);
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public long getCartItemCount(Long userId) {
        return cartItemRepository.countByUserUserId(userId);
    }

    private boolean isVariantValid(ProductVariant variant) {
        if (variant == null || variant.getDeletedAt() != null) {
            return false;
        }
        if (variant.getProduct() == null || variant.getProduct().getDeletedAt() != null) {
            return false;
        }
        return variant.getProduct().getStatus() == ProductStatus.ACTIVE;
    }

    private CartResponse buildCartResponse(List<CartItem> cartItems, int removedStaleCount) {
        if (cartItems.isEmpty()) {
            return CartResponse.builder()
                    .items(Collections.emptyList())
                    .totalItems(0)
                    .totalQuantity(0)
                    .totalAmount(BigDecimal.ZERO)
                    .originalTotalAmount(BigDecimal.ZERO)
                    .savingsAmount(BigDecimal.ZERO)
                    .removedStaleItemsCount(removedStaleCount)
                    .build();
        }

        // Batch Query for available stock across all distinct variantIds (Zero N+1)
        List<Long> variantIds = cartItems.stream()
                .map(item -> item.getVariant().getVariantId())
                .distinct()
                .toList();

        List<VariantStockSummaryProjection> stockProjections = inventoryRepository.findAvailableStockByVariantIds(variantIds);
        Map<Long, Long> stockMap = stockProjections.stream()
                .collect(Collectors.toMap(
                        VariantStockSummaryProjection::getVariantId,
                        VariantStockSummaryProjection::getAvailableQty,
                        (existing, replacement) -> existing
                ));

        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal originalTotalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (CartItem item : cartItems) {
            ProductVariant variant = item.getVariant();
            BigDecimal price = (variant.getSalePrice() != null && variant.getSalePrice().compareTo(BigDecimal.ZERO) > 0)
                    ? variant.getSalePrice()
                    : variant.getPrice();
            BigDecimal originalPrice = variant.getPrice();

            Long availableQty = stockMap.getOrDefault(variant.getVariantId(), 0L);
            boolean isAvailable = availableQty > 0;
            boolean isExceededStock = item.getQuantity() > availableQty;

            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal originalSubtotal = originalPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            totalAmount = totalAmount.add(subtotal);
            originalTotalAmount = originalTotalAmount.add(originalSubtotal);
            totalQuantity += item.getQuantity();

            String imageUrl = resolveImageUrl(variant);

            itemResponses.add(CartItemResponse.builder()
                    .cartId(item.getCartId())
                    .variantId(variant.getVariantId())
                    .variantName(variant.getVariantName())
                    .skuVariant(variant.getSkuVariant())
                    .price(price)
                    .originalPrice(originalPrice)
                    .imageUrl(imageUrl)
                    .productId(variant.getProduct().getProductId())
                    .productName(variant.getProduct().getName())
                    .productSlug(variant.getProduct().getSlug())
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .availableQty(availableQty)
                    .isAvailable(isAvailable)
                    .isExceededStock(isExceededStock)
                    .build());
        }

        BigDecimal savingsAmount = originalTotalAmount.subtract(totalAmount).max(BigDecimal.ZERO);

        return CartResponse.builder()
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .originalTotalAmount(originalTotalAmount)
                .savingsAmount(savingsAmount)
                .removedStaleItemsCount(removedStaleCount)
                .build();
    }

    private String resolveImageUrl(ProductVariant variant) {
        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            for (ProductImage img : variant.getImages()) {
                if (img.getImageUrl() != null && !img.getImageUrl().trim().isEmpty()) {
                    return img.getImageUrl();
                }
            }
        }
        if (variant.getProduct() != null && variant.getProduct().getImages() != null && !variant.getProduct().getImages().isEmpty()) {
            for (ProductImage img : variant.getProduct().getImages()) {
                if (img.getImageUrl() != null && !img.getImageUrl().trim().isEmpty()) {
                    return img.getImageUrl();
                }
            }
        }
        return null;
    }
}
