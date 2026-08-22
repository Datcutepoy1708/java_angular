package com.store.entity.product;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProductStatusConverter implements AttributeConverter<ProductStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProductStatus attribute) {
        if (attribute == null) {
            return ProductStatus.ACTIVE.getValue();
        }
        return attribute.getValue();
    }

    @Override
    public ProductStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return ProductStatus.ACTIVE;
        }
        return ProductStatus.fromValue(dbData);
    }
}
