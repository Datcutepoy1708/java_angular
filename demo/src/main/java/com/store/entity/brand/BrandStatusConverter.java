package com.store.entity.brand;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class BrandStatusConverter implements AttributeConverter<BrandStatus, String> {

    @Override
    public String convertToDatabaseColumn(BrandStatus status) {
        return status == null ? null : status.getValue();
    }

    @Override
    public BrandStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BrandStatus.fromValue(dbData);
    }
}
