package com.faiq.navanbillingservice.transaction.domain.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faiq.navanbillingservice.transaction.domain.NavanFeeKind;
import jakarta.validation.constraints.NotNull;

public record NavanFeeMetadata(
        @NotNull @JsonProperty("fee_kind") NavanFeeKind feeKind,
        String description,
        @JsonProperty("related_external_id") String relatedExternalId
) implements TransactionMetadata {}