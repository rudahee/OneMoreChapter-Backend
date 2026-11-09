package org.rudahee.backend.catalog.web.dto;

import org.rudahee.backend.catalog.domain.MediaUnit;

import java.time.LocalDate;

public record MediaUnitResponse(
        Long id,
        Integer seasonNumber,
        int unitNumber,
        String title,
        LocalDate airDate,
        Integer runtimeMinutes) {

    public static MediaUnitResponse from(MediaUnit unit) {
        // El centinela 0 es un detalle de persistencia; hacia fuera vuelve a
        // ser null, que es lo que significa "esta obra no tiene temporadas".
        Integer season = unit.getSeasonNumber() == MediaUnit.NO_SEASON
                ? null
                : unit.getSeasonNumber();
        return new MediaUnitResponse(unit.getId(), season, unit.getUnitNumber(),
                unit.getTitle(), unit.getAirDate(), unit.getRuntimeMinutes());
    }
}
