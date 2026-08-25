package com.store.entity.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PaymentMethod {
    COD("cod"),
    BANK_TRANSFER("bank_transfer"),
    VNPAY("vnpay"),
    MOMO("momo"),
    ZALOPAY("zalopay");

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PaymentMethod fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return COD;
        }
        for (PaymentMethod method : PaymentMethod.values()) {
            if (method.value.equalsIgnoreCase(value.trim()) || method.name().equalsIgnoreCase(value.trim())) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown payment method: " + value);
    }
}
