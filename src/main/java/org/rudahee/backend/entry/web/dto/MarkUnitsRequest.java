package org.rudahee.backend.entry.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @param unitIds  unidades sueltas a marcar
 * @param upToUnitId marca todo hasta esa unidad inclusive. Es el caso real:
 *                   os haceis cinco episodios y luego os acordais de marcar
 */
public record MarkUnitsRequest(
        @Size(max = 500) List<Long> unitIds,
        Long upToUnitId) {

    @AssertTrue(message = "Indica unitIds o upToUnitId, no ambos")
    @JsonIgnore
    public boolean isValidCombination() {
        boolean hasList = unitIds != null && !unitIds.isEmpty();
        return hasList ^ (upToUnitId != null);
    }
}
