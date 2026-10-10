package com.faiq.navanbillingservice.invoice.api;

import java.math.BigDecimal;

public record FeeRollupResponse(
        String name,
        BigDecimal amount
) {}