package com.store.entity.product;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ImageTypeConverter implements AttributeConverter<ImageType, String> {

    @Override
    public String convertToDatabaseColumn(ImageType attribute) {
        if (attribute == null) {
            return ImageType.SUB.getValue();
        }
        return attribute.getValue();
    }

    @Override
    public ImageType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return ImageType.SUB;
        }
        return ImageType.fromValue(dbData);
    }
}
