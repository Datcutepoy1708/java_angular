package com.store.entity.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ProductVariantStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    ProductVariantStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProductVariantStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ACTIVE;
        }
        for (ProductVariantStatus status : ProductVariantStatus.values()) {
            if (status.value.equalsIgnoreCase(value.trim()) || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid variant status: " + value + ". Allowed values are 'active' or 'inactive'.");
    }
}
