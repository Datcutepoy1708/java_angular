package com.store.entity.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TokenType {
    REFRESH_TOKEN("refresh_token"),
    RESET_PASSWORD("reset_password"),
    VERIFY_EMAIL("verify_email");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TokenType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return REFRESH_TOKEN;
        }
        for (TokenType type : TokenType.values()) {
            if (type.value.equalsIgnoreCase(value.trim()) || type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid token type: " + value);
    }
}
