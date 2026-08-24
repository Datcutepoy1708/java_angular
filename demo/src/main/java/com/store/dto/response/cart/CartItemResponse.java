package com.store.dto.response.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private Long cartId;
    private Long variantId;
    private String variantName;
    private String skuVariant;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String imageUrl;
    private Long productId;
    private String productName;
    private String productSlug;
    private Integer quantity;
    private BigDecimal subtotal;
    private Long availableQty;
    private Boolean isAvailable;
    private Boolean isExceededStock;
}
