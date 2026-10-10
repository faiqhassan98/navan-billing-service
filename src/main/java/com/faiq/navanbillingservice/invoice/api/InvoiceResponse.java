package com.faiq.navanbillingservice.invoice.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InvoiceResponse(
        String id,
        String currency,
        BigDecimal total,
        @JsonProperty("tax_total")   BigDecimal taxTotal,
        @JsonProperty("fee_total")   BigDecimal feeTotal,
        List<InvoiceLineResponse> lines,
        @JsonProperty("tax_rollup")  List<TaxRollupResponse> taxRollup,
        @JsonProperty("fee_rollup")  List<FeeRollupResponse> feeRollup,
        @JsonProperty("created_at")  Instant createdAt,
        @JsonProperty("updated_at")  Instant updatedAt
) {}