package com.store.entity.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttributeDataType {
    TEXT("text"),
    NUMBER("number"),
    BOOLEAN("boolean");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AttributeDataType fromValue(String value) {
        if (value == null) return null;
        for (AttributeDataType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AttributeDataType: " + value);
    }
}
