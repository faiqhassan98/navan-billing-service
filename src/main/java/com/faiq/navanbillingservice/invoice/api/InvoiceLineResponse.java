package com.faiq.navanbillingservice.invoice.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faiq.navanbillingservice.transaction.api.FeeLineResponse;
import com.faiq.navanbillingservice.transaction.api.TaxLineResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InvoiceLineResponse(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("external_id")    String externalId,
        String type,
        String currency,
        @JsonProperty("occurred_at")    Instant occurredAt,
        BigDecimal total,
        @JsonProperty("tax_lines")      List<TaxLineResponse> taxLines,
        @JsonProperty("fee_lines")      List<FeeLineResponse> feeLines,
        Map<String, Object> metadata
) {}