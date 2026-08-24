package com.store.entity.inventory;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class InventoryChangeTypeConverter implements AttributeConverter<InventoryChangeType, String> {

    @Override
    public String convertToDatabaseColumn(InventoryChangeType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public InventoryChangeType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return InventoryChangeType.fromValue(dbData);
    }
}
