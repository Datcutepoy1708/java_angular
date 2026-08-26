package com.store.entity.returnrefund;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ReturnReason {
    DEFECTIVE("DEFECTIVE"),
    WRONG_ITEM("WRONG_ITEM"),
    DAMAGED_IN_TRANSIT("DAMAGED_IN_TRANSIT"),
    NOT_AS_DESCRIBED("NOT_AS_DESCRIBED"),
    CHANGE_OF_MIND("CHANGE_OF_MIND"),
    OTHER("OTHER");

    private final String value;

    ReturnReason(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReturnReason fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OTHER;
        }
        for (ReturnReason r : ReturnReason.values()) {
            if (r.value.equalsIgnoreCase(value.trim()) || r.name().equalsIgnoreCase(value.trim())) {
                return r;
            }
        }
        return OTHER;
    }
}
