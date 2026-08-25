package com.store.entity.banner;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BannerPositionConverter implements AttributeConverter<BannerPosition, String> {

    @Override
    public String convertToDatabaseColumn(BannerPosition attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public BannerPosition convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return BannerPosition.fromValue(dbData);
    }
}
