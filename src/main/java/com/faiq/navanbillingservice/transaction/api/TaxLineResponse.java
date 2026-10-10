package com.faiq.navanbillingservice.transaction.api;

import java.math.BigDecimal;

public record TaxLineResponse(
        String name,
        BigDecimal rate,
        BigDecimal amount
) {}