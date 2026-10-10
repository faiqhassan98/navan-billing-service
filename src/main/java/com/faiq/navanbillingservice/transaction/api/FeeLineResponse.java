package com.faiq.navanbillingservice.transaction.api;

import java.math.BigDecimal;

public record FeeLineResponse(
        String name,
        BigDecimal amount
) {}