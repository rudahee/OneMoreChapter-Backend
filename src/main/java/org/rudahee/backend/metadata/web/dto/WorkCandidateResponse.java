package org.rudahee.backend.metadata.web.dto;

import org.rudahee.backend.metadata.domain.WorkCandidate;
import org.rudahee.backend.metadata.domain.WorkType;

import java.util.Map;

/**
 * Proyeccion de {@link WorkCandidate} hacia el cliente. El dominio no se expone
 * directamente para que un cambio interno no rompa el contrato de la API.
 */
public record WorkCandidateResponse(
        String source,
        String externalId,
        WorkType type,
        String title,
        Integer year,
        String coverUrl,
        Map<String, String> externalIds) {

    public static WorkCandidateResponse from(WorkCandidate candidate) {
        return new WorkCandidateResponse(candidate.source(), candidate.externalId(),
                candidate.type(), candidate.title(), candidate.year(),
                candidate.coverUrl(), candidate.externalIds());
    }
}
