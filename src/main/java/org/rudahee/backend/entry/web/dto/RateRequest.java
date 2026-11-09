package org.rudahee.backend.entry.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RateRequest(
        @NotNull @Min(1) @Max(10) Integer score,
        @Size(max = 2000) String review) {
}
