package com.faiq.navanbillingservice.transaction.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PatchTransactionRequest(
        BigDecimal total,
        @JsonProperty("occurred_at") Instant occurredAt,
        @JsonProperty("tax_lines")   List<TaxLineRequest> taxLines,
        @JsonProperty("fee_lines")   List<FeeLineRequest> feeLines,
        JsonNode metadata
) {}