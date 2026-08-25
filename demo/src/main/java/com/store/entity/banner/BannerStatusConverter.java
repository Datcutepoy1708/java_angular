package com.store.entity.banner;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BannerStatusConverter implements AttributeConverter<BannerStatus, String> {

    @Override
    public String convertToDatabaseColumn(BannerStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public BannerStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return BannerStatus.fromValue(dbData);
    }
}
