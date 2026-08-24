package com.store.dto.response.inventory;

import com.store.entity.inventory.Inventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {

    private Long inventoryId;
    private Long variantId;
    private String variantName;
    private String skuVariant;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Long productId;
    private String productName;
    private String productSlug;
    private String productThumbnail;
    private Integer warehouseId;
    private String warehouseName;
    private Integer quantity;
    private Integer reservedQty;
    private Integer availableQty;
    private String stockStatus;
    private LocalDateTime updatedAt;

    public static InventoryResponse fromEntity(Inventory inventory) {
        if (inventory == null) return null;

        var variant = inventory.getVariant();
        var product = variant != null ? variant.getProduct() : null;
        var warehouse = inventory.getWarehouse();

        int available = inventory.getAvailableQty();
        String status = available <= 0 ? "OUT_OF_STOCK" : (available <= 10 ? "LOW_STOCK" : "IN_STOCK");

        String thumbnail = null;
        if (variant != null && variant.getImages() != null && !variant.getImages().isEmpty()) {
            thumbnail = variant.getImages().get(0).getImageUrl();
        } else if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
            thumbnail = product.getImages().get(0).getImageUrl();
        }

        return InventoryResponse.builder()
                .inventoryId(inventory.getInventoryId())
                .variantId(variant != null ? variant.getVariantId() : null)
                .variantName(variant != null ? variant.getVariantName() : null)
                .skuVariant(variant != null ? variant.getSkuVariant() : null)
                .price(variant != null ? variant.getPrice() : null)
                .salePrice(variant != null ? variant.getSalePrice() : null)
                .productId(product != null ? product.getProductId() : null)
                .productName(product != null ? product.getName() : null)
                .productSlug(product != null ? product.getSlug() : null)
                .productThumbnail(thumbnail)
                .warehouseId(warehouse != null ? warehouse.getWarehouseId() : null)
                .warehouseName(warehouse != null ? warehouse.getName() : null)
                .quantity(inventory.getQuantity())
                .reservedQty(inventory.getReservedQty())
                .availableQty(available)
                .stockStatus(status)
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
