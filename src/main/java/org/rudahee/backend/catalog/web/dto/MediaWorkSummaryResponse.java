package org.rudahee.backend.catalog.web.dto;

import org.rudahee.backend.catalog.domain.MediaWork;
import org.rudahee.backend.metadata.domain.WorkStatus;
import org.rudahee.backend.metadata.domain.WorkType;

/**
 * Proyeccion ligera para listados. No toca {@code units} ni
 * {@code externalIds}, que son colecciones perezosas: mapear el detalle
 * completo en un listado paginado dispararia un N+1.
 */
public record MediaWorkSummaryResponse(
        Long id,
        WorkType type,
        String title,
        Integer year,
        String coverUrl,
        boolean episodic,
        Integer totalUnits,
        WorkStatus status) {

    public static MediaWorkSummaryResponse from(MediaWork work) {
        return new MediaWorkSummaryResponse(work.getId(), work.getType(), work.getTitle(),
                work.getReleaseYear(), work.getCoverUrl(), work.isEpisodic(),
                work.getTotalUnits(), work.getStatus());
    }
}
