package com.store.entity.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransferType {
    IN("in"),
    OUT("out"),
    UNKNOWN("unknown");

    private final String value;

    TransferType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TransferType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNKNOWN;
        }
        for (TransferType type : values()) {
            if (type.value.equalsIgnoreCase(value.trim()) || type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
