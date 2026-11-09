package org.rudahee.backend.entry.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.rudahee.backend.entry.service.PickerService;
import org.rudahee.backend.metadata.domain.WorkType;

/**
 * @param episodic    true solo series, false solo cosas de una sentada, null ambas
 * @param onlyStarted terminar lo empezado antes de abrir frentes nuevos
 */
public record PickRequest(
        WorkType type,
        Boolean episodic,
        Boolean onlyStarted,
        @Min(1) @Max(5) Integer count) {

    public PickerService.PickCriteria toCriteria() {
        return new PickerService.PickCriteria(
                type,
                episodic,
                Boolean.TRUE.equals(onlyStarted),
                count == null ? 1 : count);
    }
}
