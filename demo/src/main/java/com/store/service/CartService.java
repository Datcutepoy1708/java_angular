package com.store.service;

import com.store.dto.request.cart.AddToCartRequest;
import com.store.dto.request.cart.MergeCartRequest;
import com.store.dto.response.cart.CartResponse;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addToCart(Long userId, AddToCartRequest request);

    CartResponse updateQuantity(Long userId, Long cartItemId, Integer quantity);

    CartResponse removeItem(Long userId, Long cartItemId);

    void clearCart(Long userId);

    CartResponse mergeGuestCart(Long userId, MergeCartRequest request);

    long getCartItemCount(Long userId);
}
