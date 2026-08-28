package com.store.entity.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AuthProvider {
    LOCAL("local"),
    GOOGLE("google"),
    FACEBOOK("facebook"),
    ZALO("zalo");

    private final String value;

    AuthProvider(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AuthProvider fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LOCAL;
        }
        for (AuthProvider p : AuthProvider.values()) {
            if (p.value.equalsIgnoreCase(value.trim()) || p.name().equalsIgnoreCase(value.trim())) {
                return p;
            }
        }
        throw new IllegalArgumentException("Invalid auth provider: " + value);
    }
}
