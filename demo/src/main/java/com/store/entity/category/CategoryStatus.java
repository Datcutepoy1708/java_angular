package com.store.entity.category;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CategoryStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    CategoryStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CategoryStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ACTIVE;
        }
        for (CategoryStatus status : CategoryStatus.values()) {
            if (status.value.equalsIgnoreCase(value.trim()) || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid category status: " + value + ". Allowed values are 'active' or 'inactive'.");
    }
}
