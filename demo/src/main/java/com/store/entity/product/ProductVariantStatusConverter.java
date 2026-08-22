package com.store.entity.product;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProductVariantStatusConverter implements AttributeConverter<ProductVariantStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProductVariantStatus attribute) {
        if (attribute == null) {
            return ProductVariantStatus.ACTIVE.getValue();
        }
        return attribute.getValue();
    }

    @Override
    public ProductVariantStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return ProductVariantStatus.ACTIVE;
        }
        return ProductVariantStatus.fromValue(dbData);
    }
}
