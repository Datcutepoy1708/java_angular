package com.store.entity.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ImageType {
    MAIN("main"),
    SUB("sub");

    private final String value;

    ImageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ImageType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SUB;
        }
        for (ImageType type : ImageType.values()) {
            if (type.value.equalsIgnoreCase(value.trim()) || type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid image type: " + value + ". Allowed values are 'main' or 'sub'.");
    }
}
