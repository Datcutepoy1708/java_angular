package com.store.entity.discount;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DiscountStatusConverter implements AttributeConverter<DiscountStatus, String> {

    @Override
    public String convertToDatabaseColumn(DiscountStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public DiscountStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return DiscountStatus.fromValue(dbData);
    }
}
