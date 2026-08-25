package com.store.entity.review;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReviewStatusConverter implements AttributeConverter<ReviewStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReviewStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public ReviewStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return ReviewStatus.valueOf(dbData.toUpperCase());
    }
}
