package com.faiq.navanbillingservice.transaction.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faiq.navanbillingservice.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TransactionResponse(
        String id,
        @JsonProperty("external_id") String externalId,
        TransactionType type,
        String currency,
        @JsonProperty("occurred_at") Instant occurredAt,
        BigDecimal total,
        @JsonProperty("tax_lines") List<TaxLineResponse> taxLines,
        @JsonProperty("fee_lines") List<FeeLineResponse> feeLines,
        Map<String, Object> metadata,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {}