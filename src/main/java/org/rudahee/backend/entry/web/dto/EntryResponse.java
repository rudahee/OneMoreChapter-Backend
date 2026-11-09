package org.rudahee.backend.entry.web.dto;

import org.rudahee.backend.catalog.web.dto.MediaWorkResponse;
import org.rudahee.backend.entry.domain.Entry;
import org.rudahee.backend.entry.domain.EntryStatus;

import java.time.Instant;
import java.util.List;

/**
 * Ficha completa: obra con sus unidades, progreso, valoraciones y etiquetas.
 *
 * Las notas no vienen aqui: pueden ser cientos en un manga largo y se piden
 * aparte, filtradas por unidad.
 */
public record EntryResponse(
        Long id,
        EntryStatus status,
        int priority,
        Long addedByUserId,
        Instant startedAt,
        Instant finishedAt,
        MediaWorkResponse work,
        ProgressResponse progress,
        List<Long> seenUnitIds,
        List<RatingResponse> ratings,
        List<TagResponse> tags) {

    public static EntryResponse from(Entry entry, ProgressResponse progress,
                                     List<Long> seenUnitIds,
                                     List<RatingResponse> ratings) {
        return new EntryResponse(entry.getId(), entry.getStatus(), entry.getPriority(),
                entry.getAddedBy().getId(), entry.getStartedAt(), entry.getFinishedAt(),
                MediaWorkResponse.from(entry.getWork()), progress, seenUnitIds,
                ratings, entry.getTags().stream().map(TagResponse::from).toList());
    }
}
