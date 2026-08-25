package com.store.entity.banner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BannerStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    BannerStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BannerStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (BannerStatus status : BannerStatus.values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown BannerStatus: " + value);
    }
}
