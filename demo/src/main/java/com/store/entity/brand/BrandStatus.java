package com.store.entity.brand;

public enum BrandStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    BrandStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BrandStatus fromValue(String value) {
        for (BrandStatus s : values()) {
            if (s.value.equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("Unknown BrandStatus: " + value);
    }
}
