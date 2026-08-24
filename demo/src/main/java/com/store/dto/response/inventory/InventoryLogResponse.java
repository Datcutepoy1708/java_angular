package com.store.dto.response.inventory;

import com.store.entity.inventory.InventoryChangeType;
import com.store.entity.inventory.InventoryLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLogResponse {

    private Long logId;
    private Long variantId;
    private String variantName;
    private String skuVariant;
    private Long productId;
    private String productName;
    private Integer warehouseId;
    private String warehouseName;
    private InventoryChangeType changeType;
    private Integer quantityChange;
    private String referenceType;
    private Long referenceId;
    private String note;
    private Long createdByUserId;
    private String createdByUserName;
    private LocalDateTime createdAt;

    public static InventoryLogResponse fromEntity(InventoryLog log) {
        if (log == null) return null;

        var variant = log.getVariant();
        var product = variant != null ? variant.getProduct() : null;
        var warehouse = log.getWarehouse();
        var user = log.getCreatedBy();

        return InventoryLogResponse.builder()
                .logId(log.getLogId())
                .variantId(variant != null ? variant.getVariantId() : null)
                .variantName(variant != null ? variant.getVariantName() : null)
                .skuVariant(variant != null ? variant.getSkuVariant() : null)
                .productId(product != null ? product.getProductId() : null)
                .productName(product != null ? product.getName() : null)
                .warehouseId(warehouse != null ? warehouse.getWarehouseId() : null)
                .warehouseName(warehouse != null ? warehouse.getName() : null)
                .changeType(log.getChangeType())
                .quantityChange(log.getQuantityChange())
                .referenceType(log.getReferenceType())
                .referenceId(log.getReferenceId())
                .note(log.getNote())
                .createdByUserId(user != null ? user.getUserId() : null)
                .createdByUserName(user != null ? user.getFullName() : "System")
                .createdAt(log.getCreatedAt())
                .build();
    }
}
