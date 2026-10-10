package com.faiq.navanbillingservice.transaction.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TaxLineRequest(
        @NotBlank String name,
        BigDecimal rate,        // nullable
        @NotNull  BigDecimal amount
) {}