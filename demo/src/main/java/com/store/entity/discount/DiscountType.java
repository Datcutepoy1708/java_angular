package com.store.entity.discount;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DiscountType {
    PERCENT("percent"),
    FIXED("fixed");

    private final String value;

    DiscountType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DiscountType fromValue(String value) {
        if (value == null) return null;
        for (DiscountType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown discount type: " + value);
    }
}
