package com.store.entity.payment;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransferTypeConverter implements AttributeConverter<TransferType, String> {

    @Override
    public String convertToDatabaseColumn(TransferType attribute) {
        if (attribute == null) {
            return TransferType.UNKNOWN.getValue();
        }
        return attribute.getValue();
    }

    @Override
    public TransferType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return TransferType.UNKNOWN;
        }
        return TransferType.fromValue(dbData);
    }
}
