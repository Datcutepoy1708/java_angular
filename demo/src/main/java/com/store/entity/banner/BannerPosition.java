package com.store.entity.banner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BannerPosition {
    HOMEPAGE_SLIDER("homepage_slider"),
    SIDEBAR("sidebar"),
    POPUP("popup"),
    CATEGORY_TOP("category_top");

    private final String value;

    BannerPosition(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BannerPosition fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (BannerPosition pos : BannerPosition.values()) {
            if (pos.value.equalsIgnoreCase(value) || pos.name().equalsIgnoreCase(value)) {
                return pos;
            }
        }
        throw new IllegalArgumentException("Unknown BannerPosition: " + value);
    }
}
