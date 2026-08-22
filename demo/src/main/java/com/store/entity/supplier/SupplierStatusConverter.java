package com.store.entity.supplier;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SupplierStatusConverter implements AttributeConverter<SupplierStatus, String> {

    @Override
    public String convertToDatabaseColumn(SupplierStatus attribute) {
        if (attribute == null) {
            return SupplierStatus.ACTIVE.getValue();
        }
        return attribute.getValue();
    }

    @Override
    public SupplierStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return SupplierStatus.ACTIVE;
        }
        return SupplierStatus.fromValue(dbData);
    }
}
