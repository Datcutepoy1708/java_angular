package com.store.entity.returnrefund;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ReturnStatus {
    REQUESTED("REQUESTED"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    ITEM_RECEIVED("ITEM_RECEIVED"),
    REFUNDED("REFUNDED"),
    CANCELLED("CANCELLED");

    private final String value;

    ReturnStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReturnStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return REQUESTED;
        }
        for (ReturnStatus s : ReturnStatus.values()) {
            if (s.value.equalsIgnoreCase(value.trim()) || s.name().equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        return REQUESTED;
    }
}
