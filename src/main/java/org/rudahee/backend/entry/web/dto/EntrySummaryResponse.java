package org.rudahee.backend.entry.web.dto;

import org.rudahee.backend.catalog.web.dto.MediaWorkSummaryResponse;
import org.rudahee.backend.entry.domain.Entry;
import org.rudahee.backend.entry.domain.EntryStatus;

import java.time.Instant;
import java.util.List;

/**
 * Proyeccion de listado.
 *
 * Sin progreso: calcularlo exige contar unidades vistas por entrada, y en un
 * listado paginado eso es una consulta por fila. El progreso se pide al abrir
 * la ficha.
 */
public record EntrySummaryResponse(
        Long id,
        EntryStatus status,
        int priority,
        Instant startedAt,
        Instant finishedAt,
        MediaWorkSummaryResponse work,
        List<TagResponse> tags) {

    public static EntrySummaryResponse from(Entry entry) {
        return new EntrySummaryResponse(entry.getId(), entry.getStatus(),
                entry.getPriority(), entry.getStartedAt(), entry.getFinishedAt(),
                MediaWorkSummaryResponse.from(entry.getWork()),
                entry.getTags().stream().map(TagResponse::from).toList());
    }
}
