package com.store.entity.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuthProviderConverter implements AttributeConverter<AuthProvider, String> {

    @Override
    public String convertToDatabaseColumn(AuthProvider attribute) {
        if (attribute == null) {
            return AuthProvider.LOCAL.getValue();
        }
        return attribute.getValue();
    }

    @Override
    public AuthProvider convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return AuthProvider.LOCAL;
        }
        return AuthProvider.fromValue(dbData);
    }
}
