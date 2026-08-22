package com.store.entity.category;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CategoryStatusConverter implements AttributeConverter<CategoryStatus, String> {

    @Override
    public String convertToDatabaseColumn(CategoryStatus attribute) {
        if (attribute == null) {
            return CategoryStatus.ACTIVE.getValue();
        }
        return attribute.getValue();
    }

    @Override
    public CategoryStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return CategoryStatus.ACTIVE;
        }
        return CategoryStatus.fromValue(dbData);
    }
}
