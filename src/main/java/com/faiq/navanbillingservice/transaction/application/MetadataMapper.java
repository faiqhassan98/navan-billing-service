package com.faiq.navanbillingservice.transaction.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faiq.navanbillingservice.transaction.domain.NavanFeeKind;
import com.faiq.navanbillingservice.transaction.domain.TransactionType;
import com.faiq.navanbillingservice.transaction.domain.metadata.*;
import org.springframework.stereotype.Component;

@Component
public class MetadataMapper {

    private final ObjectMapper objectMapper;

    public MetadataMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses and validates the raw metadata JsonNode into a typed record.
     * Throws IllegalArgumentException (→ 400) for unknown types or fee kinds.
     */
    public TransactionMetadata parse(TransactionType type, JsonNode metadata) {
        try {
            return switch (type) {
                case FLIGHT    -> objectMapper.treeToValue(metadata, FlightMetadata.class);
                case HOTEL     -> objectMapper.treeToValue(metadata, HotelMetadata.class);
                case RAIL      -> objectMapper.treeToValue(metadata, RailMetadata.class);
                case NAVAN_FEE -> {
                    NavanFeeMetadata fee =
                        objectMapper.treeToValue(metadata, NavanFeeMetadata.class);
                    // validate fee_kind explicitly so unknown kinds return 400
                    if (fee.feeKind() == null) {
                        throw new IllegalArgumentException(
                            "navan_fee metadata must include a valid fee_kind " +
                            "(trip_fee or agent_call_fee)");
                    }
                    yield fee;
                }
            };
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid metadata for type " + type.getValue() + ": " + e.getMessage());
        }
    }

    /**
     * Converts any typed metadata record back to a plain Map for storage/response.
     */
    @SuppressWarnings("unchecked")
    public java.util.Map<String, Object> toMap(TransactionMetadata metadata) {
        return objectMapper.convertValue(metadata, java.util.Map.class);
    }
}