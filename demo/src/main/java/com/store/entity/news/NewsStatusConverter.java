package com.store.entity.news;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NewsStatusConverter implements AttributeConverter<NewsStatus, String> {

    @Override
    public String convertToDatabaseColumn(NewsStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public NewsStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return NewsStatus.fromValue(dbData);
    }
}
