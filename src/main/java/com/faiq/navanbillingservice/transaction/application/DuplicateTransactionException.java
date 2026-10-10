package com.faiq.navanbillingservice.transaction.application;

public class DuplicateTransactionException extends RuntimeException {

    private final String existingId;

    public DuplicateTransactionException(String existingId) {
        super("Duplicate external_id — existing transaction id: " + existingId);
        this.existingId = existingId;
    }

    public String getExistingId() {
        return existingId;
    }
}