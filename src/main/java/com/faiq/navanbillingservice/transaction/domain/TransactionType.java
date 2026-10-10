package com.faiq.navanbillingservice.transaction.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {
    FLIGHT("flight"),
    HOTEL("hotel"),
    RAIL("rail"),
    NAVAN_FEE("navan_fee");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static TransactionType fromValue(String value) {
        for (TransactionType t : values()) {
            if (t.value.equals(value)) return t;
        }
        throw new IllegalArgumentException("Unknown transaction type: " + value);
    }
}