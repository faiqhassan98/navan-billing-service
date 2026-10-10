package com.faiq.navanbillingservice.transaction.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CreateTransactionRequest(
        @NotBlank String type,
        @NotBlank @JsonProperty("external_id") String externalId,
        @NotBlank String currency,
        @NotNull  @JsonProperty("occurred_at") Instant occurredAt,
        @NotNull  BigDecimal total,
        @NotNull  @Valid @JsonProperty("tax_lines") List<TaxLineRequest> taxLines,
        @NotNull  @Valid @JsonProperty("fee_lines") List<FeeLineRequest> feeLines,
        @NotNull  JsonNode metadata
) {}