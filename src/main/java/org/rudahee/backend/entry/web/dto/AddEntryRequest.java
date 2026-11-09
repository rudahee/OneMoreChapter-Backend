package org.rudahee.backend.entry.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import org.rudahee.backend.metadata.domain.WorkType;

/**
 * Anyadir una obra a la lista, de dos maneras.
 *
 * Con {@code workId} si ya esta en el catalogo; con
 * {@code source + externalId + type} si viene recien buscada en los
 * proveedores, y entonces se importa de paso. Admitir ambas evita obligar al
 * cliente a encadenar dos llamadas en el caso mas comun.
 */
public record AddEntryRequest(
        Long workId,
        @Size(max = 32) String source,
        @Size(max = 128) String externalId,
        WorkType type) {

    @JsonIgnore
    public boolean isFromCatalog() {
        return workId != null;
    }

    @AssertTrue(message = "Indica workId, o bien source, externalId y type")
    @JsonIgnore
    public boolean isValidCombination() {
        boolean fromProvider = source != null && externalId != null && type != null;
        // Exactamente una de las dos formas: aceptar ambas a la vez dejaria sin
        // definir cual gana.
        return isFromCatalog() ^ fromProvider;
    }
}
