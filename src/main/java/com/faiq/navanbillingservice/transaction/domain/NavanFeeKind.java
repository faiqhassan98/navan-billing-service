package com.faiq.navanbillingservice.transaction.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NavanFeeKind {
    TRIP_FEE("trip_fee"),
    AGENT_CALL_FEE("agent_call_fee");

    private final String value;

    NavanFeeKind(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static NavanFeeKind fromValue(String value) {
        for (NavanFeeKind k : values()) {
            if (k.value.equals(value)) return k;
        }
        throw new IllegalArgumentException("Unknown navan_fee kind: " + value);
    }
}