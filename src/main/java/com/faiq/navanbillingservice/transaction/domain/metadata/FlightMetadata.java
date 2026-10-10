package com.faiq.navanbillingservice.transaction.domain.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FlightMetadata(
        @NotBlank String origin,
        @NotBlank String destination,
        @NotNull  BigDecimal fare,
        @NotEmpty @Valid List<Coupon> coupons
) implements TransactionMetadata {

    public record Coupon(
            @NotBlank @JsonProperty("flight_number") String flightNumber,
            String carrier,
            @NotBlank String from,
            @NotBlank String to,
            @NotNull  Instant departure,
            @NotNull  Instant arrival
    ) {}
}