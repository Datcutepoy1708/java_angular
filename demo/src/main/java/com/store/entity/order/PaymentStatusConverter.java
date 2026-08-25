package com.store.entity.order;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(PaymentStatus attribute) {
        if (attribute == null) {
            return PaymentStatus.UNPAID.getValue();
        }
        return attribute.getValue();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return PaymentStatus.UNPAID;
        }
        return PaymentStatus.fromValue(dbData);
    }
}
