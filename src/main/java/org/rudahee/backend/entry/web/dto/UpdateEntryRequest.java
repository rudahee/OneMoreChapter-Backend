package org.rudahee.backend.entry.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.rudahee.backend.entry.domain.EntryStatus;

/** Campos opcionales: se aplica solo lo que venga informado. */
public record UpdateEntryRequest(
        EntryStatus status,
        @Min(0) @Max(5) Integer priority) {
}
