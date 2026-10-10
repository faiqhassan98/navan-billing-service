package com.faiq.navanbillingservice.transaction.domain.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HotelMetadata(
        @NotBlank String name,
        @NotNull @Valid Address address,
        @NotNull  @JsonProperty("check_in")  LocalDate checkIn,
        @NotNull  @JsonProperty("check_out") LocalDate checkOut,
        @NotNull  Integer nights,
        @NotNull  @JsonProperty("room_rate_per_night") BigDecimal roomRatePerNight,
        List<Extra> extras
) implements TransactionMetadata {

    public record Address(
            @NotBlank String street,
            @NotBlank String city,
            @NotBlank String country
    ) {}

    public record Extra(
            @NotBlank String name,
            @NotNull  BigDecimal amount
    ) {}
}