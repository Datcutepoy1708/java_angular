package com.store.entity.product;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AttributeDataTypeConverter implements AttributeConverter<AttributeDataType, String> {

    @Override
    public String convertToDatabaseColumn(AttributeDataType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AttributeDataType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AttributeDataType.fromValue(dbData);
    }
}
