package com.store.entity.discount;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DiscountStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    EXPIRED("expired");

    private final String value;

    DiscountStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DiscountStatus fromValue(String value) {
        if (value == null) return null;
        for (DiscountStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown discount status: " + value);
    }
}
