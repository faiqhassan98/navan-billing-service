package com.faiq.navanbillingservice.transaction.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PageResponse<T>(
        List<T> data,
        @JsonProperty("total_elements") long totalElements,
        @JsonProperty("total_pages")    int totalPages,
        @JsonProperty("page_number")    int pageNumber,
        @JsonProperty("page_size")      int pageSize
) {}