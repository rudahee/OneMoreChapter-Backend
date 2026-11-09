package org.rudahee.backend.metadata.domain;

import org.rudahee.backend.common.util.Texts;

import java.util.Map;

/**
 * Resultado de busqueda: lo minimo para pintar una lista y que el usuario elija.
 *
 * @param source      clave del proveedor de origen
 * @param externalIds identificadores cruzados ya conocidos, usados en el merge
 */
public record WorkCandidate(
        String source,
        String externalId,
        WorkType type,
        String title,
        Integer year,
        String coverUrl,
        double score,
        Map<String, String> externalIds) {

    public WorkCandidate {
        externalIds = externalIds == null ? Map.of() : Map.copyOf(externalIds);
    }

    /** Clave de deduplicacion cuando no hay identificadores cruzados que casar. */
    public String dedupeKey() {
        return Texts.normalize(title) + '|' + (year == null ? "?" : year) + '|' + type;
    }
}
