package com.store.entity.order;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {

    @Override
    public String convertToDatabaseColumn(PaymentMethod attribute) {
        if (attribute == null) {
            return PaymentMethod.COD.getValue();
        }
        return attribute.getValue();
    }

    @Override
    public PaymentMethod convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return PaymentMethod.COD;
        }
        return PaymentMethod.fromValue(dbData);
    }
}
