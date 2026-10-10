package com.faiq.navanbillingservice.invoice.api;

import java.math.BigDecimal;

public record TaxRollupResponse(
        String name,
        BigDecimal rate,
        BigDecimal amount
) {}