package org.rudahee.backend.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.rudahee.backend.metadata.domain.WorkType;

/**
 * Peticion de importacion. Llega con lo que devolvio la busqueda de metadatos:
 * el cliente elige un candidato y manda su procedencia, no la obra entera.
 * Aceptar los datos del cliente permitiria envenenar el catalogo compartido.
 */
public record ImportWorkRequest(
        @NotBlank @Size(max = 32) String source,
        @NotBlank @Size(max = 128) String externalId,
        @NotNull WorkType type) {
}
