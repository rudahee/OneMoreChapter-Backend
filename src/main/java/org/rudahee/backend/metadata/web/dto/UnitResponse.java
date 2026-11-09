package org.rudahee.backend.metadata.web.dto;

import org.rudahee.backend.metadata.domain.UnitDetail;

import java.time.LocalDate;

public record UnitResponse(
        Integer seasonNumber,
        int unitNumber,
        String title,
        LocalDate airDate,
        Integer runtimeMinutes) {

    public static UnitResponse from(UnitDetail unit) {
        return new UnitResponse(unit.seasonNumber(), unit.unitNumber(), unit.title(),
                unit.airDate(), unit.runtimeMinutes());
    }
}