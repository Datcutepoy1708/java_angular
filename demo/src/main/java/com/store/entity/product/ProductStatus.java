package com.store.entity.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ProductStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    DISCONTINUED("discontinued");

    private final String value;

    ProductStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProductStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ACTIVE;
        }
        for (ProductStatus status : ProductStatus.values()) {
            if (status.value.equalsIgnoreCase(value.trim()) || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid product status: " + value + ". Allowed values are 'active', 'inactive', or 'discontinued'.");
    }
}
