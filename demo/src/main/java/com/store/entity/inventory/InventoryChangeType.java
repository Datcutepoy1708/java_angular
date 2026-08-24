package com.store.entity.inventory;

import lombok.Getter;

@Getter
public enum InventoryChangeType {
    IMPORT("import"),
    SALE("sale"),
    RETURN("return"),
    ADJUST("adjust"),
    TRANSFER("transfer");

    private final String value;

    InventoryChangeType(String value) {
        this.value = value;
    }

    public static InventoryChangeType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (InventoryChangeType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown inventory change type: " + value);
    }
}
