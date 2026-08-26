package com.store.entity.returnrefund;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ItemCondition {
    NEW_SEAL("NEW_SEAL"),
    OPENED("OPENED"),
    DAMAGED("DAMAGED"),
    DEFECTIVE("DEFECTIVE");

    private final String value;

    ItemCondition(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ItemCondition fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OPENED;
        }
        for (ItemCondition c : ItemCondition.values()) {
            if (c.value.equalsIgnoreCase(value.trim()) || c.name().equalsIgnoreCase(value.trim())) {
                return c;
            }
        }
        return OPENED;
    }
}
