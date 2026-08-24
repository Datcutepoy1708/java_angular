package com.store.dto.response.attribute;

import com.store.entity.product.Attribute;
import com.store.entity.product.AttributeDataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeResponse implements Serializable {

    private Integer attributeId;
    private Integer categoryId;
    private String categoryName;
    private String name;
    private AttributeDataType dataType;
    private String unit;
    private Integer sortOrder;

    public static AttributeResponse fromEntity(Attribute entity) {
        if (entity == null) return null;
        return AttributeResponse.builder()
                .attributeId(entity.getAttributeId())
                .categoryId(entity.getCategory() != null ? entity.getCategory().getCategoryId() : null)
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .name(entity.getName())
                .dataType(entity.getDataType())
                .unit(entity.getUnit())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
