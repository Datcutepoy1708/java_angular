package com.store.dto.response.order;

import com.store.entity.order.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long orderItemId;
    private Long variantId;
    private String variantName;
    private String skuVariant;
    private Long productId;
    private String productName;
    private String productSlug;
    private String imageUrl;
    private Integer warehouseId;
    private String warehouseName;
    private String productNameSnapshot;
    private BigDecimal priceSnapshot;
    private Integer quantity;
    private BigDecimal subtotal;

    public static OrderItemResponse fromEntity(OrderItem item, String imageUrl) {
        if (item == null) return null;
        var variant = item.getVariant();
        var product = variant != null ? variant.getProduct() : null;
        var warehouse = item.getWarehouse();

        return OrderItemResponse.builder()
                .orderItemId(item.getOrderItemId())
                .variantId(variant != null ? variant.getVariantId() : null)
                .variantName(variant != null ? variant.getVariantName() : null)
                .skuVariant(variant != null ? variant.getSkuVariant() : null)
                .productId(product != null ? product.getProductId() : null)
                .productName(product != null ? product.getName() : null)
                .productSlug(product != null ? product.getSlug() : null)
                .imageUrl(imageUrl)
                .warehouseId(warehouse != null ? warehouse.getWarehouseId() : null)
                .warehouseName(warehouse != null ? warehouse.getName() : null)
                .productNameSnapshot(item.getProductNameSnapshot())
                .priceSnapshot(item.getPriceSnapshot())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
