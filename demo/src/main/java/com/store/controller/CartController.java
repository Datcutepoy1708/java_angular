package com.store.controller;

import com.store.dto.request.cart.AddToCartRequest;
import com.store.dto.request.cart.MergeCartRequest;
import com.store.dto.request.cart.UpdateCartItemRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.cart.CartResponse;
import com.store.security.CustomUserDetails;
import com.store.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping Cart Management APIs (Zero-holding soft staging)")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's shopping cart with up-to-date prices and stock status")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        CartResponse response = cartService.getCart(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin giỏ hàng thành công", response));
    }

    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add an item to the shopping cart (accumulates quantity if item already exists)")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddToCartRequest request) {
        CartResponse response = cartService.addToCart(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Thêm vào giỏ hàng thành công", response));
    }

    @PutMapping("/items/{cartItemId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update quantity of a cart item (auto-removes item if quantity <= 0)")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse response = cartService.updateQuantity(userDetails.getUserId(), cartItemId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng thành công", response));
    }

    @DeleteMapping("/items/{cartItemId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove a single item from the cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartItemId) {
        CartResponse response = cartService.removeItem(userDetails.getUserId(), cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa sản phẩm khỏi giỏ hàng", response));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Clear all items from the current user's shopping cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        cartService.clearCart(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ giỏ hàng", null));
    }

    @PostMapping("/merge")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Merge guest localStorage cart items into the user's server database cart on login")
    public ResponseEntity<ApiResponse<CartResponse>> mergeGuestCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MergeCartRequest request) {
        CartResponse response = cartService.mergeGuestCart(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Đồng bộ giỏ hàng thành công", response));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get distinct item count in current user's cart")
    public ResponseEntity<ApiResponse<Long>> getCartItemCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = cartService.getCartItemCount(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng mặt hàng thành công", count));
    }
}
