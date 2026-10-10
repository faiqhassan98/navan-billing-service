package com.faiq.navanbillingservice.invoice.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateInvoiceRequest(
        @JsonProperty("transaction_id")  String transactionId,
        @JsonProperty("transaction_ids") List<String> transactionIds
) {
    /**
     * Supports both single id and list form from the brief:
     *   { "transaction_id": "txn_123" }
     *   { "transaction_ids": ["txn_123", "txn_456"] }
     */
    public List<String> resolvedIds() {
        if (transactionIds != null && !transactionIds.isEmpty()) {
            return transactionIds;
        }
        if (transactionId != null && !transactionId.isBlank()) {
            return List.of(transactionId);
        }
        throw new IllegalArgumentException(
            "Provide either transaction_id or transaction_ids");
    }
}