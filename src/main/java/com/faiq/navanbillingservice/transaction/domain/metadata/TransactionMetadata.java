package com.faiq.navanbillingservice.transaction.domain.metadata;

/**
 * Sealed marker interface for type-safe metadata.
 * Deserialization target is chosen explicitly at the controller/service
 * layer based on the outer "type" field — not by Jackson polymorphism.
 * Stored as JSONB in the database.
 */
public sealed interface TransactionMetadata
    permits FlightMetadata, HotelMetadata, RailMetadata, NavanFeeMetadata {}