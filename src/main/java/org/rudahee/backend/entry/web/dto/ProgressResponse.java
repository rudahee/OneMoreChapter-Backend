package org.rudahee.backend.entry.web.dto;

import org.rudahee.backend.entry.service.ProgressService;

/**
 * Progreso compartido. No hay desglose por persona a proposito: vais juntos.
 *
 * @param percentage null si no se conoce el total de unidades
 */
public record ProgressResponse(
        long seenCount,
        Integer totalUnits,
        Long currentUnitId,
        Integer positionSeconds,
        boolean complete,
        Integer percentage) {

    public static ProgressResponse from(ProgressService.ProgressSummary summary) {
        Integer percentage = (summary.totalUnits() == null || summary.totalUnits() == 0)
                ? null
                : (int) Math.round(100.0 * summary.seenCount() / summary.totalUnits());

        return new ProgressResponse(summary.seenCount(), summary.totalUnits(),
                summary.currentUnitId(), summary.positionSeconds(),
                summary.complete(), percentage);
    }
}
