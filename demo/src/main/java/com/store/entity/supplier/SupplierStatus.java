package com.store.entity.supplier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SupplierStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    SupplierStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SupplierStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ACTIVE;
        }
        for (SupplierStatus status : SupplierStatus.values()) {
            if (status.value.equalsIgnoreCase(value.trim()) || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid supplier status: " + value + ". Allowed values are 'active' or 'inactive'.");
    }
}
