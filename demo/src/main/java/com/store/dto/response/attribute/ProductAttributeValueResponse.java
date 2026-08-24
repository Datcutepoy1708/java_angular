package com.store.dto.response.attribute;

import com.store.entity.product.AttributeDataType;
import com.store.entity.product.ProductAttributeValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributeValueResponse implements Serializable {

    private Long id;
    private Integer attributeId;
    private String attributeName;
    private AttributeDataType dataType;
    private String unit;
    private String value;

    public static ProductAttributeValueResponse fromEntity(ProductAttributeValue entity) {
        if (entity == null) return null;
        return ProductAttributeValueResponse.builder()
                .id(entity.getId())
                .attributeId(entity.getAttribute() != null ? entity.getAttribute().getAttributeId() : null)
                .attributeName(entity.getAttribute() != null ? entity.getAttribute().getName() : null)
                .dataType(entity.getAttribute() != null ? entity.getAttribute().getDataType() : null)
                .unit(entity.getAttribute() != null ? entity.getAttribute().getUnit() : null)
                .value(entity.getValue())
                .build();
    }
}
