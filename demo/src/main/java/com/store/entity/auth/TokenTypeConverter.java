package com.store.entity.auth;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TokenTypeConverter implements AttributeConverter<TokenType, String> {

    @Override
    public String convertToDatabaseColumn(TokenType attribute) {
        if (attribute == null) {
            return TokenType.REFRESH_TOKEN.getValue();
        }
        return attribute.getValue();
    }

    @Override
    public TokenType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return TokenType.REFRESH_TOKEN;
        }
        return TokenType.fromValue(dbData);
    }
}
